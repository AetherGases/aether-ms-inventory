package com.aether.ms_inventory.inventory.dto.output;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GenerateInventoryOutputDTO(
    Integer id,
    String name,
    InventoryTypeEnum type,
    InventoryStatusEnum status,
    LocalDate periodStart,
    LocalDate periodEnd,
    LocalDateTime createdAt
) {
}
