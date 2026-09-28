package com.aether.ms_inventory.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Value("${spring.data.redis.host}")
  private String host;

  @Value("${spring.data.redis.port}")
  private int port;

  @Value("${spring.data.redis.password}")
  private String password;

  @Bean
  public JedisConnectionFactory connectionFactory() {
    RedisStandaloneConfiguration configuration =
        new RedisStandaloneConfiguration();

    configuration.setHostName(host);
    configuration.setPort(port);
    configuration.setPassword(password);

    return new JedisConnectionFactory(configuration);
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(
      JedisConnectionFactory connectionFactory
  ) {
    RedisTemplate<String, Object> template =
        new RedisTemplate<>();

    template.setConnectionFactory(connectionFactory);

    StringRedisSerializer stringSerializer =
        new StringRedisSerializer();

    JacksonJsonRedisSerializer<Object> jsonSerializer =
        new JacksonJsonRedisSerializer<>(
            Object.class
        );

    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);

    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();

    return template;
  }
}