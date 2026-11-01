package com.aether.ms_inventory.shared.persistence.mongo.documents;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "notification_config")
@NoArgsConstructor
@Getter
@Setter
public class NotificationConfigDocument {
  @Id
  private String id;

  @Indexed(unique = true)
  private Integer userId;

  private Notify notify = new Notify();
  private List<Token> tokens = new ArrayList<>();

  public NotificationConfigDocument(Integer userId) {
    this.userId = userId;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Notify {
    private Boolean email;
    private Boolean mobile;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Token {
    private String token;
    private Instant createdAt;
    private Instant lastUsedAt;
  }
}
