package com.aether.ms_inventory.shared.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {
  private boolean enabled;

  /** JSON da service account do Firebase codificado em base64. */
  private String credentials;
}
