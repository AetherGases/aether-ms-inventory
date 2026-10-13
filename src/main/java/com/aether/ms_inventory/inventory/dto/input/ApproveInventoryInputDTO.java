package com.aether.ms_inventory.inventory.dto.input;

public record ApproveInventoryInputDTO(
    Integer id,
    Integer validatorId,
    String message
) {
}
