package com.aether.ms_inventory.inventory.dto.output;

import java.math.BigDecimal;

public record FindUnitInfoOutputDTO(
    BigDecimal totalReductions,
    BigDecimal totalEmissions
) {
}
