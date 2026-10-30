package com.aether.ms_inventory.shared.persistence.redis.repositories;

import com.aether.ms_inventory.shared.persistence.redis.entities.InputInventoryDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class InputInventoryRepository {

  private static final String KEY_PREFIX = "input_inventories:";
  private static final String LOCK_PREFIX = "input_inventories_lock:";
  private static final String ATTEMPTS_PREFIX = "input_inventories_attempts:";
  private static final Duration ATTEMPTS_TTL = Duration.ofDays(1);

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

  public List<Integer> findAllIds(int limit) {
    List<Integer> ids = new ArrayList<>();

    ScanOptions options = ScanOptions.scanOptions()
        .match(KEY_PREFIX + "*")
        .count(100)
        .build();

    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      while (cursor.hasNext()) {
        String key = cursor.next();

        try {
          ids.add(Integer.valueOf(key.substring(KEY_PREFIX.length())));
        } catch (NumberFormatException ignored) {}
      }
    }

    return ids.stream().distinct().sorted().limit(limit).toList();
  }

  public boolean tryLock(Integer id, Duration ttl) {
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(LOCK_PREFIX + id, "locked", ttl);

    return Boolean.TRUE.equals(acquired);
  }

  public void unlock(Integer id) {
    redisTemplate.delete(LOCK_PREFIX + id);
  }

  public void delayRetry(Integer id, Duration delay) {
    redisTemplate.expire(LOCK_PREFIX + id, delay);
  }

  public long incrementAttempts(Integer id) {
    String key = ATTEMPTS_PREFIX + id;
    Long attempts = redisTemplate.opsForValue().increment(key);
    redisTemplate.expire(key, ATTEMPTS_TTL);

    return attempts == null ? 1L : attempts;
  }

  public void clearAttempts(Integer id) {
    redisTemplate.delete(ATTEMPTS_PREFIX + id);
  }
}