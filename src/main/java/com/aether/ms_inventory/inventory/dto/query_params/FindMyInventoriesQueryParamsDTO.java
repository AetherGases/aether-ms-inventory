package com.aether.ms_inventory.inventory.dto.query_params;

import com.aether.ms_inventory.shared.AetherConstants;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FindMyInventoriesQueryParamsDTO(
    String name,

    InventoryStatusEnum status,

    @Min(value = 1, message = "{validation.take.min-value}")
    @Max(value = AetherConstants.MAX_TAKE, message = "{validation.take.max-value}")
    Integer take,

    @Min(value = 1, message = "{validation.skip.min-value}")
    Integer skip
) {
}
