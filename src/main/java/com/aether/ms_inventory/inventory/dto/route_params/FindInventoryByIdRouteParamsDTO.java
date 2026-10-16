package com.aether.ms_inventory.inventory.dto.route_params;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FindInventoryByIdRouteParamsDTO(
    @NotNull(message = "{validation.inventory-id.required}")
    @Min(value = 1, message = "{validation.inventory-id.min-value}")
    @Schema(
        description = "ID do inventário",
        example = "1"
    )
    Integer id
) {
}