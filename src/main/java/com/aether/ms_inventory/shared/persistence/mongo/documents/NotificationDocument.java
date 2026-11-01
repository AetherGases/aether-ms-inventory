package com.aether.ms_inventory.shared.persistence.mongo.documents;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notifications")
@NoArgsConstructor
@Getter
@Setter
public class NotificationDocument {
  @Id
  private String id;

  @Indexed
  private Integer userId;
  private Inventory inventory;
  private Employee validatorEmployee;
  private String title;
  private String description;

  @Indexed(expireAfter = "2d")
  private Instant createdAt;

  @NoArgsConstructor
  @AllArgsConstructor
  @Getter
  @Setter
  public static class Inventory{
    private Integer id;
    private InventoryStatusEnum status;
    private InventoryTypeEnum type;
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Getter
  @Setter
  public static class Employee{
    private Integer id;
    private String name;
  }
}
