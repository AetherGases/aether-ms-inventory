package com.aether.ms_inventory.seal.dto.output;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record FindSealProgressOutputDTO(
    @Schema(description = "Pontuação total somada dos critérios", example = "0.70")
    BigDecimal totalPoints,

    @Schema(description = "Pontuação mínima para obter o selo (categoria D)", example = "1.60")
    BigDecimal minimumPoints,

    @Schema(description = "Categoria atual (A, B, C ou D). Nulo se ainda não atingiu o mínimo", example = "D", nullable = true)
    String category,

    @Schema(description = "Próxima categoria a alcançar. Nulo se já está na categoria A", example = "C", nullable = true)
    String nextCategory,

    @Schema(description = "Pontos que faltam para a próxima categoria. Nulo se já está na categoria A", example = "0.90", nullable = true)
    BigDecimal pointsToNextCategory,

    @Schema(description = "Percentual de progresso até a próxima categoria (0 a 100)", example = "43.75")
    BigDecimal progressPercent,

    @Schema(description = "Quantidade de critérios atingidos (com pontuação maior que zero)", example = "1")
    int criteriaMet,

    @Schema(description = "Quantidade total de critérios do selo", example = "4")
    int criteriaTotal,

    List<Criterion> criteria
) {
  public record Criterion(
      @Schema(description = "Identificador do critério", example = "GHG_DECLARATION")
      String code,

      @Schema(description = "Nome do critério", example = "Declaração de GEE")
      String name,

      @Schema(description = "Pontos obtidos no critério", example = "0.70")
      BigDecimal points,

      @Schema(description = "Pontuação máxima do critério. Nulo quando não há teto fixo", example = "1.00", nullable = true)
      BigDecimal maxPoints,

      @Schema(description = "Indica se o sistema possui dados para calcular este critério", example = "true")
      boolean tracked
  ) {
  }
}
