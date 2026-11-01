package com.aether.ms_inventory.shared.workers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InventoryOutputContext(
    InventoryData inventory,
    EnterpriseData enterprise,
    ResponsibleData responsible,
    List<UnitData> units,
    List<DeclarationCellData> declarationCells,
    List<ReductionData> reductions
) {

  public record InventoryData(
      Integer id,
      String name,
      String description,
      String consolidationApproach,
      LocalDate periodStart,
      LocalDate periodEnd
  ) {}

  public record EnterpriseData(
      String legalName,
      String tradeName,
      String cnpj,
      AddressData address,
      String cnae,
      Integer employeeCount,
      BigDecimal annualRevenue,
      String companySize,
      String marketModality
  ) {}

  public record AddressData(
      String zipCode,
      String state,
      String city,
      String neighborhood,
      String street,
      Integer number,
      String complement
  ) {}

  public record ResponsibleData(
      String name,
      String position,
      String phone,
      String mobile,
      String email
  ) {}

  public record UnitData(
      String cnpj,
      String legalName,
      String fullAddress,
      String city,
      boolean emissionsQuantified,
      String type
  ) {}

  // Célula da Declaração Completa devolvida por fn_ghg_complete_declaration.
  public record DeclarationCellData(
      String frame,
      String axis,
      String code,
      BigDecimal value
  ) {}

  public record ReductionData(
      String category,
      BigDecimal quantityCo2e
  ) {}
}