package com.aether.ms_inventory.worker;

import com.aether.ms_inventory.shared.exceptions.InventoryProcessingException;
import com.aether.ms_inventory.shared.persistence.redis.repositories.InputInventoryRepository;
import com.aether.ms_inventory.shared.services.InventoryProcessingPersistence;
import com.aether.ms_inventory.shared.workers.InventoryProcessor;
import com.aether.ms_inventory.shared.workers.InventoryWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryWorker Tests")
class InventoryWorkerTests {

  @Mock private InputInventoryRepository queue;
  @Mock private InventoryProcessor processor;
  @Mock private InventoryProcessingPersistence persistence;

  private InventoryWorker worker(int maxAttempts) {
    return new InventoryWorker(queue, processor, persistence, 10, maxAttempts, 30, 300);
  }

  @Test
  @DisplayName("Should skip inventories whose lock could not be acquired")
  void skipsLockedInventory() {
    when(queue.findAllIds(10)).thenReturn(List.of(1));
    when(queue.tryLock(1, Duration.ofSeconds(300))).thenReturn(false);

    worker(3).poll();

    verify(processor, never()).process(any());
  }

  @Test
  @DisplayName("Should process, remove from the queue and release the lock on success")
  void processesSuccessfully() {
    when(queue.findAllIds(10)).thenReturn(List.of(1));
    when(queue.tryLock(1, Duration.ofSeconds(300))).thenReturn(true);

    worker(3).poll();

    verify(processor).process(1);
    verify(queue).deleteById(1);
    verify(queue).clearAttempts(1);
    verify(queue).unlock(1);
    verify(queue, never()).delayRetry(any(), any());
  }

  @Test
  @DisplayName("Should keep the lock and schedule a retry on a retryable failure under the attempt limit")
  void retriesRetryableFailure() {
    when(queue.findAllIds(10)).thenReturn(List.of(1));
    when(queue.tryLock(1, Duration.ofSeconds(300))).thenReturn(true);
    when(queue.incrementAttempts(1)).thenReturn(1L);
    doThrow(new InventoryProcessingException("falha temporária", true))
        .when(processor).process(1);

    worker(3).poll();

    verify(queue).delayRetry(1, Duration.ofSeconds(30));
    verify(queue, never()).unlock(1);
    verify(queue, never()).deleteById(any());
    verify(persistence, never()).markFailed(any(), any());
  }

  @Test
  @DisplayName("Should give up and mark as failed once the retry limit is reached")
  void givesUpAfterMaxAttempts() {
    when(queue.findAllIds(10)).thenReturn(List.of(1));
    when(queue.tryLock(1, Duration.ofSeconds(300))).thenReturn(true);
    when(queue.incrementAttempts(1)).thenReturn(3L);
    doThrow(new InventoryProcessingException("falha persistente", true))
        .when(processor).process(1);

    worker(3).poll();

    verify(persistence).markFailed(1, "falha persistente");
    verify(queue).deleteById(1);
    verify(queue).clearAttempts(1);
    verify(queue).unlock(1);
    verify(queue, never()).delayRetry(any(), any());
  }

  @Test
  @DisplayName("Should mark as failed immediately on a non-retryable failure")
  void failsImmediatelyOnNonRetryable() {
    when(queue.findAllIds(10)).thenReturn(List.of(1));
    when(queue.tryLock(1, Duration.ofSeconds(300))).thenReturn(true);
    when(queue.incrementAttempts(1)).thenReturn(1L);
    doThrow(new InventoryProcessingException("arquivo inválido", false))
        .when(processor).process(1);

    worker(3).poll();

    verify(persistence).markFailed(1, "arquivo inválido");
    verify(queue).deleteById(1);
    verify(queue).unlock(1);
  }

  @Test
  @DisplayName("Should mark insufficient data and extract the blank fields when the AI reports HTTP 422")
  void marksInsufficientData() {
    when(queue.findAllIds(10)).thenReturn(List.of(1));
    when(queue.tryLock(1, Duration.ofSeconds(300))).thenReturn(true);
    when(queue.incrementAttempts(1)).thenReturn(1L);

    String message = "Dados insuficientes ```inventory {\"owner\": null, \"description\": \"\", "
        + "\"gases\": [], \"valid\": \"ok\"}``` ";
    doThrow(new InventoryProcessingException(message, false, HttpStatus.UNPROCESSABLE_CONTENT))
        .when(processor).process(1);

    worker(3).poll();

    verify(persistence).markInsufficientData(1, List.of("owner", "description", "gases"));
    verify(persistence, never()).markFailed(any(), any());
    verify(queue).deleteById(1);
  }

  @Test
  @DisplayName("Should wrap an unexpected exception as retryable and still respect the attempt limit")
  void wrapsUnexpectedException() {
    when(queue.findAllIds(10)).thenReturn(List.of(1));
    when(queue.tryLock(1, Duration.ofSeconds(300))).thenReturn(true);
    when(queue.incrementAttempts(1)).thenReturn(1L);
    doThrow(new IllegalStateException("boom")).when(processor).process(1);

    worker(3).poll();

    verify(queue).delayRetry(1, Duration.ofSeconds(30));
    verify(queue, never()).unlock(1);
  }

  @Test
  @DisplayName("Should keep the lock and schedule a retry when persisting the failure itself fails")
  void retainsLockWhenMarkingFailureFails() {
    when(queue.findAllIds(10)).thenReturn(List.of(1));
    when(queue.tryLock(1, Duration.ofSeconds(300))).thenReturn(true);
    when(queue.incrementAttempts(1)).thenReturn(3L);
    doThrow(new InventoryProcessingException("falha persistente", true))
        .when(processor).process(1);
    doThrow(new RuntimeException("db down")).when(persistence).markFailed(1, "falha persistente");

    worker(3).poll();

    verify(queue).delayRetry(1, Duration.ofSeconds(30));
    verify(queue, never()).unlock(1);
    verify(queue, never()).deleteById(any());
  }

  @Test
  @DisplayName("Should not propagate an error while reading the queue")
  void pollSwallowsQueueReadErrors() {
    when(queue.findAllIds(10)).thenThrow(new RuntimeException("redis down"));

    assertDoesNotThrow(() -> worker(3).poll());

    verify(processor, never()).process(any());
  }
}
