package com.aether.ms_inventory.shared.config;

import com.aether.ms_inventory.shared.config.properties.BrevoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BrevoProperties.class)
@RequiredArgsConstructor
public class BrevoConfig {

  private final BrevoProperties properties;

  @Bean
  public RestClient client(RestClient.Builder builder) {
    return builder
        .baseUrl(properties.getApi().getUrl())
        .defaultHeader("api-key", properties.getApi().getKey())
        .build();
  }
}