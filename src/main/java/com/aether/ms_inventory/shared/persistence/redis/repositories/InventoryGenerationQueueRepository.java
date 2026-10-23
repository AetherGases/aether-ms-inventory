package com.aether.ms_inventory.shared.persistence.redis.repositories;

import com.aether.ms_inventory.shared.workers.InventoryGenerationJob;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class InventoryGenerationQueueRepository {

  public static final String QUEUE_KEY = "inventory_generation_queue";

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public InventoryGenerationQueueRepository(
      RedisConnectionFactory connectionFactory,
      ObjectMapper objectMapper
  ) {
    this.stringRedisTemplate = new StringRedisTemplate(connectionFactory);
    this.objectMapper = objectMapper;
  }

  public void enqueue(InventoryGenerationJob job) {
    try {
      String payload = objectMapper.writeValueAsString(job);

      stringRedisTemplate.opsForList().rightPush(QUEUE_KEY, payload);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Falha ao serializar a tarefa de geração do inventário", e);
    }
  }

  public InventoryGenerationJob dequeue() {
    String payload = stringRedisTemplate.opsForList().leftPop(QUEUE_KEY);

    if (payload == null) {
      return null;
    }

    try {
      return objectMapper.readValue(payload, InventoryGenerationJob.class);
    } catch (JsonProcessingException e) {
      log.error("Falha ao desserializar a tarefa de geração do inventário: {}", payload, e);
      return null;
    }
  }
}
