package com.aether.ms_inventory.shared.workers;

import com.aether.ms_inventory.shared.enums.StorageFileTypeEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.StorageFileRepository;
import com.aether.ms_inventory.shared.services.CloudinaryUploadService;
import com.aether.ms_inventory.shared.services.InventoryGenerationPersistence;
import com.aether.ms_inventory.shared.services.InventoryGenerationSpreadsheetFiller;
import com.aether.ms_inventory.shared.workers.contexts.InventoryOutputContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryGenerationProcessor {
  private final InventoryGenerationPersistence persistence;
  private final InventoryGenerationSpreadsheetFiller spreadsheetFiller;
  private final CloudinaryUploadService cloudinaryUploadService;
  private final StorageFileRepository storageFileRepository;

  public void process(Integer inventoryId) {
    if (!persistence.markProcessing(inventoryId)) {
      return;
    }

    try {
      Optional<InventoryOutputContext> loaded = persistence.loadContext(inventoryId);

      if (loaded.isEmpty()) {
        return;
      }

      InventoryOutputContext context = loaded.get();

      byte[] spreadsheet = spreadsheetFiller.fill(context);

      String fileName = "inventario-" + inventoryId + ".xlsx";

      CloudinaryUploadService.CloudinaryUploadResult uploadResult =
          cloudinaryUploadService.upload(spreadsheet, fileName, StorageFileTypeEnum.XLSX);

      StorageFileEntity storageFile = new StorageFileEntity(fileName, uploadResult.secureUrl());
      storageFileRepository.save(storageFile);

      persistence.markIssued(inventoryId, storageFile);
    } catch (RuntimeException e) {
      log.error("Falha ao gerar o relatório de saída do inventário {}", inventoryId, e);
      persistence.markFailed(inventoryId, "Falha ao gerar o relatório de saída");
    }
  }
}
