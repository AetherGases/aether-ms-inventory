package com.aether.ms_inventory.shared.workers;

import com.aether.ms_inventory.shared.persistence.redis.repositories.InventoryGenerationQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "inventory.generation-worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InventoryGenerationWorker {
  private final InventoryGenerationQueueRepository queue;
  private final InventoryGenerationProcessor processor;

  @Scheduled(fixedDelayString = "${inventory.generation-worker.poll-interval-ms:5000}")
  public void poll() {
    InventoryGenerationJob job;

    try {
      job = queue.dequeue();
    } catch (RuntimeException e) {
      log.error("Não foi possível ler a fila de geração de inventários", e);
      return;
    }

    if (job == null) {
      return;
    }

    try {
      processor.process(job.inventoryId());
    } catch (RuntimeException e) {
      log.error("Erro inesperado ao gerar o inventário {}", job.inventoryId(), e);
    }
  }
}
