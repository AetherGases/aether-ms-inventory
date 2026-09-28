package com.aether.ms_inventory.shared.persistence.redis.repositories;

import com.aether.ms_inventory.shared.persistence.redis.entities.InputInventoryDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public class InputInventoryRepository {

  private static final String KEY_PREFIX = "input_inventories:";

  private final RedisTemplate<String, Object> redisTemplate;
  private final HashOperations<String, String, Object> hashOperations;
  private final ObjectMapper objectMapper;

  public InputInventoryRepository(
      RedisTemplate<String, Object> redisTemplate,
      ObjectMapper objectMapper
  ) {
    this.redisTemplate = redisTemplate;
    this.hashOperations = redisTemplate.opsForHash();
    this.objectMapper = objectMapper;
  }

  public InputInventoryDocument save(InputInventoryDocument document) {
    String key = KEY_PREFIX + document.getId();

    Map<String, Object> fields = objectMapper.convertValue(
        document, new TypeReference<Map<String, Object>>() {}
    );

    hashOperations.putAll(key, fields);

    return document;
  }

  public InputInventoryDocument findById(Integer id) {
    Map<String, Object> data = hashOperations.entries(KEY_PREFIX + id);

    if (data.isEmpty()) {
      return null;
    }

    return objectMapper.convertValue(data, InputInventoryDocument.class);
  }

  public void deleteById(Integer id) {
    redisTemplate.delete(KEY_PREFIX + id);
  }
}