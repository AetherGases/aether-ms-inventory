package com.aether.ms_inventory.worker;

import com.aether.ms_inventory.shared.clients.AekoClient;
import com.aether.ms_inventory.shared.dto.output.AekoReportOutputDTO;
import com.aether.ms_inventory.shared.dto.request.AekoReportRequestDTO;
import com.aether.ms_inventory.shared.services.InventoryProcessingPersistence;
import com.aether.ms_inventory.shared.services.RemoteFileDownloader;
import com.aether.ms_inventory.shared.services.XlsxMarkdownConverter;
import com.aether.ms_inventory.shared.workers.InventoryProcessingContext;
import com.aether.ms_inventory.shared.workers.InventoryProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryProcessor Tests")
class InventoryProcessorTests {

  @Mock private InventoryProcessingPersistence persistence;
  @Mock private RemoteFileDownloader fileDownloader;
  @Mock private XlsxMarkdownConverter markdownConverter;
  @Mock private AekoClient aekoClient;

  @InjectMocks private InventoryProcessor processor;

  private InventoryProcessingContext context() {
    return new InventoryProcessingContext(
        1, "https://files.test/a.xlsx", 10, 20,
        List.of(new AekoReportRequestDTO.Gas(1, "CO2")),
        List.of(new AekoReportRequestDTO.Scope(1, "Escopo 1")),
        List.of(new AekoReportRequestDTO.Category(1, "Combustão", "DIRECT"))
    );
  }

  @Test
  @DisplayName("Should do nothing when there is no context to process")
  void skipsWhenNoContext() {
    when(persistence.loadContext(1)).thenReturn(Optional.empty());

    processor.process(1);

    verify(fileDownloader, never()).download(any());
    verify(markdownConverter, never()).convert(any());
    verify(aekoClient, never()).generateReport(any());
    verify(persistence, never()).saveResult(any(), any());
  }

  @Test
  @DisplayName("Should download the file, convert it to markdown, call the AI and save the result")
  void processesInventory() {
    InventoryProcessingContext context = context();
    when(persistence.loadContext(1)).thenReturn(Optional.of(context));

    byte[] file = "xlsx-bytes".getBytes();
    when(fileDownloader.download(context.fileUrl())).thenReturn(file);
    when(markdownConverter.convert(file)).thenReturn("| a | b |");

    AekoReportOutputDTO response = new AekoReportOutputDTO("desc", null, null, List.of());
    when(aekoClient.generateReport(any())).thenReturn(response);

    processor.process(1);

    ArgumentCaptor<AekoReportRequestDTO> captor = ArgumentCaptor.forClass(AekoReportRequestDTO.class);
    verify(aekoClient).generateReport(captor.capture());

    AekoReportRequestDTO sent = captor.getValue();
    assertEquals(1, sent.idExternalContextInventory());
    assertEquals("| a | b |", sent.inventory());
    assertEquals(10, sent.idExternalUser());
    assertEquals(20, sent.idExternalCompany());
    assertEquals(context.gases(), sent.gases());
    assertEquals(context.scopes(), sent.scopes());
    assertEquals(context.categories(), sent.categories());

    verify(persistence).saveResult(1, response);
  }
}
