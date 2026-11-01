package com.aether.ms_inventory.inventory.dto.output;

import java.math.BigDecimal;
import java.util.List;

public record FindInventoryAnalysisOutputDTO(
    BigDecimal totalEmissions,
    BigDecimal changePercent,
    List<Scope> scopes,
    Long monthlySavings
) {
  public record Scope(
      String name,
      BigDecimal percentage
  ){}
}
