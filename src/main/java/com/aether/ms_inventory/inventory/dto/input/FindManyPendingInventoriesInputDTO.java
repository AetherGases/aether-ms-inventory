package com.aether.ms_inventory.inventory.dto.input;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;

public record FindManyPendingInventoriesInputDTO(
    Integer userId,
    String name,
    Integer skip,
    Integer take
) {
}
