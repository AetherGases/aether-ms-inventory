package com.aether.ms_inventory.inventory.dto.input;

public record RejectInventoryInputDTO(
    Integer id,
    Integer validatorId,
    String message
) {
}
