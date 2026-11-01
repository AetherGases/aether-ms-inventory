package com.aether.ms_inventory.shared.config;

import com.aether.ms_inventory.shared.config.properties.FirebaseProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * So e criado com firebase.enabled=true. Sem ele, o PushNotificationService
 * apenas registra em log (mesmo comportamento do Brevo desativado).
 */
@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class FirebaseConfig {

  @Bean
  public FirebaseMessaging firebaseMessaging(FirebaseProperties properties) throws IOException {
    byte[] json = Base64.getDecoder().decode(properties.getCredentials());

    FirebaseOptions options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(json)))
        .build();

    FirebaseApp app = FirebaseApp.getApps().isEmpty()
        ? FirebaseApp.initializeApp(options)
        : FirebaseApp.getInstance();

    return FirebaseMessaging.getInstance(app);
  }
}
