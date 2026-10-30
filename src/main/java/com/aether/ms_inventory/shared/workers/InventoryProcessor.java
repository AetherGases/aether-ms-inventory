package com.aether.ms_inventory.shared.workers;

import com.aether.ms_inventory.shared.clients.AekoClient;
import com.aether.ms_inventory.shared.dto.output.AekoReportOutputDTO;
import com.aether.ms_inventory.shared.dto.request.AekoReportRequestDTO;
import com.aether.ms_inventory.shared.services.InventoryProcessingPersistence;
import com.aether.ms_inventory.shared.services.RemoteFileDownloader;
import com.aether.ms_inventory.shared.services.XlsxMarkdownConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryProcessor {
  private final InventoryProcessingPersistence persistence;
  private final RemoteFileDownloader fileDownloader;
  private final XlsxMarkdownConverter markdownConverter;
  private final AekoClient aekoClient;

  public void process(Integer inventoryId) {
    Optional<InventoryProcessingContext> loaded = persistence.loadContext(inventoryId);

    if (loaded.isEmpty())
      return;

    InventoryProcessingContext context = loaded.get();

    byte[] file = fileDownloader.download(context.fileUrl());
    String markdown = markdownConverter.convert(file);

    AekoReportRequestDTO request = new AekoReportRequestDTO(
        context.inventoryId(),
        markdown,
        context.userId(),
        context.idExternalCompany(),
        context.gases(),
        context.scopes(),
        context.categories()
    );

    AekoReportOutputDTO response = aekoClient.generateReport(request);

    persistence.saveResult(inventoryId, response);
  }
}
