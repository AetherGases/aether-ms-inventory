package com.aether.ms_inventory.shared.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "brevo")
public class BrevoProperties {

  private boolean enabled;
  private Api api;
  private Sender sender;

  @Getter
  @Setter
  public static class Api {
    private String key;
    private String url;
  }

  @Getter
  @Setter
  public static class Sender {
    private String email;
    private String name;
  }
}
