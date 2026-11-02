package com.aether.ms_inventory.shared.enums;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Categorias do Selo Clima Paraná (Termo de Referência - Organizações Privadas, 2025).
 * Declaradas da menor para a maior pontuação mínima.
 */
@Getter
public enum SealCategoryEnum {
  D(new BigDecimal("1.60")),
  C(new BigDecimal("3.00")),
  B(new BigDecimal("7.00")),
  A(new BigDecimal("11.00"));

  private final BigDecimal minimumPoints;

  SealCategoryEnum(BigDecimal minimumPoints) {
    this.minimumPoints = minimumPoints;
  }
}
