package com.aether.ms_inventory.inventory.dto.output;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FindInventoryByIdOutputDTO(
    Integer id,
    String name,
    String description,
    String consolidationApproach,
    LocalDate inventoryPeriodStart,
    LocalDate inventoryPeriodEnd,
    InventoryTypeEnum type,
    InventoryStatusEnum status,
    String validationMessage,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Employee owner,
    Employee validator,
    StorageFile storageFile
) {
  public record Employee(
      Integer id,
      String name,
      String imageUrl
  ){}

  public record StorageFile(
      Integer id,
      String fileName,
      String path,
      LocalDateTime createdAt
  ){}
}