package com.aether.ms_inventory.inventory.dto.input;

public record RegisterInventoryInputDTO(
    Integer userId,
    String name,
    String fileName,
    String path
) {
}
