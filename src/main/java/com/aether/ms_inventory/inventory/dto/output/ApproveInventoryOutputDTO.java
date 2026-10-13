package com.aether.ms_inventory.inventory.dto.output;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;

import java.time.LocalDateTime;

public record ApproveInventoryOutputDTO(
    Integer id,
    String name,
    String description,
    InventoryTypeEnum type,
    InventoryStatusEnum status,
    String validationMessage,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
