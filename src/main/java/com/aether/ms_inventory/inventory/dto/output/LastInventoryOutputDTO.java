package com.aether.ms_inventory.inventory.dto.output;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LastInventoryOutputDTO(
    Integer id,
    String name,
    Author author,
    Period period,
    InventoryStatusEnum status,
    InventoryTypeEnum type,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
  public record Author(
      String name,
      String imageUrl
  ){}

  public record Period(
      LocalDate start,
      LocalDate end
  ){}
}
