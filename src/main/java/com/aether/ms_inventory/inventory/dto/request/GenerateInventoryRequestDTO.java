package com.aether.ms_inventory.inventory.dto.request;

import com.aether.ms_inventory.shared.AetherConstants;
import com.aether.ms_inventory.shared.helpers.RegexPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GenerateInventoryRequestDTO(
    @NotEmpty(message = "{validation.name.required}")
    @Size(
        min = AetherConstants.MIN_INVENTORY_NAME_LENGTH,
        max = AetherConstants.MAX_INVENTORY_NAME_LENGTH,
        message = "{validation.name.size}"
    )
    @Pattern(
        regexp = RegexPatterns.NAME,
        message = "{validation.name.regex}"
    )
    @Schema(
        description = "O nome do relatório",
        example = "Relatório de Teste"
    )
    String name,

    @NotEmpty(message = "{validation.inventory-consolidation-approach.required}")
    @Size(
        min = AetherConstants.MIN_INVENTORY_CONSOLIDATION_APPROACH_LENGTH,
        max = AetherConstants.MAX_INVENTORY_CONSOLIDATION_APPROACH_LENGTH,
        message = "{validation.inventory-consolidation-approach.size}"
    )
    @Schema(
        description = "A abordagem GHG",
        example = "Participação acionária"
    )
    String consolidationApproach,

    @NotNull(message = "{validation.start-date-period.required}")
    @Schema(
        description = "A data de início do período do inventário",
        example = "2025-01-01"
    )
    LocalDate startDatePeriod,

    @NotNull(message = "{validation.end-date-period.required}")
    @Schema(
        description = "A data de término do período do inventário",
        example = "2025-12-31"
    )
    LocalDate endDatePeriod
) {
}
