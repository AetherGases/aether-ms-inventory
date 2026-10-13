package com.aether.ms_inventory.inventory.dto.request;

import com.aether.ms_inventory.shared.AetherConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record RejectInventoryRequestDTO(
    @NotBlank(message = "{validation.inventory-validation-message.required}")
    @Size(
        min = AetherConstants.MIN_INVENTORY_STATUS_MESSAGE_LENGTH,
        max = AetherConstants.MAX_INVENTORY_STATUS_MESSAGE_LENGTH,
        message = "{validation.inventory-validation-message.size}"
    )
    @Schema(
        description = "A mensagem que descreve o motivo da reprovação",
        example = "Relatório com informações não coerentes."
    )
    String message
) {
}
