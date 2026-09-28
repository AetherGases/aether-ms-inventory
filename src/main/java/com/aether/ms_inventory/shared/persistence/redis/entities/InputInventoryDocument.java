package com.aether.ms_inventory.shared.persistence.redis.entities;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("input_inventories")
@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
public class InputInventoryDocument {
  @Id
  private Integer id;
  private InventoryStatusEnum status;
}