package com.aether.ms_inventory.shared.workers;

import com.aether.ms_inventory.shared.dto.request.AekoReportRequestDTO;

import java.util.List;

public record InventoryProcessingContext(
    Integer inventoryId,
    String fileUrl,
    Integer userId,
    List<AekoReportRequestDTO.Gas> gases,
    List<AekoReportRequestDTO.Scope> scopes,
    List<AekoReportRequestDTO.Category> categories
) {}
