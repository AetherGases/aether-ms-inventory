package com.aether.ms_inventory.inventory.dto.output;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;

import java.time.LocalDateTime;

public record RegisterInventoryOutputDTO(
    Integer id,
    String name,
    InventoryTypeEnum type,
    InventoryStatusEnum status,
    LocalDateTime createdAt,
    StorageFile storageFile
) {
  public record StorageFile(
      Integer id,
      String fileName,
      String path,
      LocalDateTime createdAt
  ){}
}
