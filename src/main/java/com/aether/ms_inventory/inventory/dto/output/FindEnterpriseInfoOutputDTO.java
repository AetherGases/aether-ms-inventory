package com.aether.ms_inventory.inventory.dto.output;

import java.math.BigDecimal;

public record FindEnterpriseInfoOutputDTO(
    BigDecimal totalReductions,
    BigDecimal totalEmissions
) {
}
