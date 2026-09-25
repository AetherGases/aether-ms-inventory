package com.aether.ms_inventory.inventory.dto.output;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FindMyInventoriesHistoryOutputDTO(
    List<Inventory> inventories,
    long totalCount
) {
  public record Inventory(
      Integer id,
      String name,
      LocalDateTime createdAt,

      InventoryStatusEnum status,
      InventoryTypeEnum type
  ){

  }
}