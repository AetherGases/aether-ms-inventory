package com.aether.ms_inventory.inventory.dto.request;

import com.aether.ms_inventory.shared.AetherConstants;
import com.aether.ms_inventory.shared.helpers.RegexPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record RegisterInventoryRequestDTO(
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

    @NotEmpty(message = "{validation.file-name.required}")
    @Size(
        min = AetherConstants.MIN_STORAGE_FILE_NAME_LENGTH,
        max = AetherConstants.MAX_STORAGE_FILE_NAME_LENGTH,
        message = "{validation.file-name.size}"
    )
    @Schema(
        description = "É o título do arquivo .xlsx do relatório.",
        example = "inventario.xlsx"
    )
    String fileName,
    @NotEmpty(message = "{validation.file-path.required}")
    @URL(
        protocol = "https",
        message = "{validation.file-path.regex}"
    )
    @Schema(
        description = "É o caminho do arquivo hospedado no Cloudinary.",
        example = "https://cloudinary/exemplo/123excel-teste.xlsx"
    )
    String path
) {
}
