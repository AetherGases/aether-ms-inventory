package com.aether.ms_inventory.inventory.dto.request;

import com.aether.ms_inventory.shared.AetherConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveInventoryRequestDTO(
    @NotBlank(message = "{validation.inventory-validation-message.required}")
    @Size(
        min = AetherConstants.MIN_INVENTORY_STATUS_MESSAGE_LENGTH,
        max = AetherConstants.MAX_INVENTORY_STATUS_MESSAGE_LENGTH,
        message = "{validation.inventory-validation-message.size}"
    )
    @Schema(
        description = "A mensagem que descreve o porquê da aprovação",
        example = "Relatório detalhado, ele irá ajudar muito na nossa análise e melhoria quanto as emissões"
    )
    String message
) {
}
