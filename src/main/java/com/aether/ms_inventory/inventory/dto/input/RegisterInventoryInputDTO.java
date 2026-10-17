package com.aether.ms_inventory.inventory.dto.input;

public record RegisterInventoryInputDTO(
    Integer userId,
    String name,
    String consolidationApproach,
    String fileName,
    String path
) {
}
