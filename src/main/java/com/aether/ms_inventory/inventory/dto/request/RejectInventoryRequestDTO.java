package com.aether.ms_inventory.inventory.dto.request;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record RejectInventoryRequestDTO(
    @NotEmpty(message = "{validation.inventory-validation-message.required}")
    @Size(
        min = AetherConstants.MIN_INVENTORY_STATUS_MESSAGE_LENGTH,
        max = AetherConstants.MAX_INVENTORY_STATUS_MESSAGE_LENGTH,
        message = "{validation.inventory-validation-message.size}"
    )
    String message
) {
}
