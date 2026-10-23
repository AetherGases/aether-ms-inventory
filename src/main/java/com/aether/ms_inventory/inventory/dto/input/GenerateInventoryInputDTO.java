package com.aether.ms_inventory.inventory.dto.input;

import java.time.LocalDate;

public record GenerateInventoryInputDTO(
    Integer userId,
    String name,
    String consolidationApproach,
    LocalDate periodStart,
    LocalDate periodEnd
) {
}
