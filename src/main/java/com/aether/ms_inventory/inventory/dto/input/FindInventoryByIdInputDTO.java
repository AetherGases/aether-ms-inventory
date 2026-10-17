package com.aether.ms_inventory.inventory.dto.input;

public record FindInventoryByIdInputDTO(
    Integer id,
    Integer userId
) {
}