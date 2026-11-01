package com.aether.ms_inventory.shared.dto.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AekoReportOutputDTO(
    @JsonProperty("description") String description,
    @JsonProperty("start_period") String startPeriod,
    @JsonProperty("end_period") String endPeriod,
    @JsonProperty("emissions") List<Emission> emissions
) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Emission(
      @JsonProperty("quantity_co2e") BigDecimal quantityCo2e,
      @JsonProperty("methodology_description") String methodologyDescription,
      @JsonProperty("supplier_data_percentage") BigDecimal supplierDataPercentage,
      @JsonProperty("gas") Integer gas,
      @JsonProperty("scope") Integer scope,
      @JsonProperty("category") Integer category,
      @JsonProperty("is_upstream") Boolean upstream,
      @JsonProperty("is_reduction") Boolean reduction
  ) {}
}
