package com.aether.ms_inventory.inventory.dto.query_params;

import com.aether.ms_inventory.shared.AetherConstants;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FindManyPendingInventoriesQueryParamsDTO(
    @Schema(
        description = "O filtro de nome dos relatórios",
        example = "Inventário"
    )
    String name,

    @Min(value = 1, message = "{validation.take.min-value}")
    @Max(value = AetherConstants.MAX_TAKE, message = "{validation.take.max-value}")
    @Schema(
        description = "Quantos relatórios deve retornar",
        example = "6"
    )
    Integer take,

    @Min(value = 0, message = "{validation.skip.min-value}")
    @Schema(
        description = "Quantos relatórios deve pular",
        example = "0"
    )
    Integer skip
) {
}
