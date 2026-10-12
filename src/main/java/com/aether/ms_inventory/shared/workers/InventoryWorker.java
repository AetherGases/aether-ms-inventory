package com.aether.ms_inventory.shared.workers;

import com.aether.ms_inventory.shared.exceptions.InventoryProcessingException;
import com.aether.ms_inventory.shared.persistence.redis.repositories.InputInventoryRepository;
import com.aether.ms_inventory.shared.services.InventoryProcessingPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "inventory.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InventoryWorker {

  private final InputInventoryRepository queue;
  private final InventoryProcessor processor;
  private final InventoryProcessingPersistence persistence;
  private final int batchSize;
  private final int maxAttempts;
  private final Duration retryDelay;
  private final Duration lockTtl;

  public InventoryWorker(
      InputInventoryRepository queue,
      InventoryProcessor processor,
      InventoryProcessingPersistence persistence,
      @Value("${inventory.worker.batch-size:10}") int batchSize,
      @Value("${inventory.worker.max-attempts:3}") int maxAttempts,
      @Value("${inventory.worker.retry-delay-seconds:30}") long retryDelaySeconds,
      @Value("${inventory.worker.lock-ttl-seconds:300}") long lockTtlSeconds
  ) {
    this.queue = queue;
    this.processor = processor;
    this.persistence = persistence;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
    this.retryDelay = Duration.ofSeconds(retryDelaySeconds);
    this.lockTtl = Duration.ofSeconds(lockTtlSeconds);
  }

  @Scheduled(fixedDelayString = "${inventory.worker.poll-interval-ms:5000}")
  public void poll() {
    List<Integer> ids;

    try {
      ids = queue.findAllIds(batchSize);
    } catch (RuntimeException e) {
      log.error("Não foi possível ler a fila de inventários", e);
      return;
    }

    for (Integer id : ids) {
      handle(id);
    }
  }

  void handle(Integer id) {
    if (!queue.tryLock(id, lockTtl)) {
      return;
    }

    boolean keepLock = false;

    try {
      processor.process(id);
      queue.deleteById(id);
      queue.clearAttempts(id);
    } catch (InventoryProcessingException e) {
      keepLock = handleFailure(id, e);
    } catch (RuntimeException e) {
      log.error("Erro inesperado ao processar o inventário {}", id, e);
      keepLock = handleFailure(
          id, new InventoryProcessingException("Erro inesperado ao processar o inventário", e, true)
      );
    } finally {
      if (!keepLock) {
        queue.unlock(id);
      }
    }
  }

  private boolean handleFailure(Integer id, InventoryProcessingException error) {
    long attempts = queue.incrementAttempts(id);

    if (error.isRetryable() && attempts < maxAttempts) {
      log.warn("Falha ao processar inventário {} (tentativa {}/{}): {}",
          id, attempts, maxAttempts, error.getMessage());
      queue.delayRetry(id, retryDelay);
      return true;
    }

    log.error("Inventário {} marcado como PROCESSING_FAILED: {}", id, error.getMessage(), error);

    try {
      persistence.markFailed(id, error.getMessage());
      queue.deleteById(id);
      queue.clearAttempts(id);
      return false;
    } catch (RuntimeException e) {
      log.error("Não foi possível marcar o inventário {} como PROCESSING_FAILED", id, e);
      queue.delayRetry(id, retryDelay);
      return true;
    }
  }
}
