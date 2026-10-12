package com.aether.ms_inventory.worker;

import com.aether.ms_inventory.shared.dto.output.AekoReportOutputDTO;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.exceptions.InventoryProcessingException;
import com.aether.ms_inventory.shared.persistence.postgres.entities.CategoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmissionEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.GasEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.ReductionEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.ScopeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.CategoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.EmissionRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.GasRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.ReductionRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.ScopeRepository;
import com.aether.ms_inventory.shared.services.InventoryProcessingPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryProcessingPersistence Tests")
class InventoryProcessingPersistenceTests {

  @Mock private InventoryRepository inventoryRepository;
  @Mock private EmissionRepository emissionRepository;
  @Mock private ReductionRepository reductionRepository;
  @Mock private GasRepository gasRepository;
  @Mock private ScopeRepository scopeRepository;
  @Mock private CategoryRepository categoryRepository;

  @InjectMocks private InventoryProcessingPersistence persistence;

  private InventoryEntity inventory;

  @BeforeEach
  void setUp() {
    inventory = new InventoryEntity();
    inventory.setId(1);
    inventory.setStatus(InventoryStatusEnum.PROCESSING);
  }

  private void stubCatalogs() {
    GasEntity gas = new GasEntity();
    gas.setId(1);
    ScopeEntity scope = new ScopeEntity();
    scope.setId(1);
    CategoryEntity category = new CategoryEntity();
    category.setId(2);

    when(gasRepository.findAll()).thenReturn(List.of(gas));
    when(scopeRepository.findAll()).thenReturn(List.of(scope));
    when(categoryRepository.findAll()).thenReturn(List.of(category));
  }

  private AekoReportOutputDTO.Emission emission(Integer gas, boolean reduction) {
    return new AekoReportOutputDTO.Emission(
        new BigDecimal("12.5"), "Fator GHG Protocol", new BigDecimal("40"),
        gas, 1, 2, false, reduction
    );
  }

  @Test
  @DisplayName("Should persist emissions and reductions and move inventory to UNDER_REVIEW")
  void saveResult() {
    stubCatalogs();
    when(inventoryRepository.findByIdForUpdate(1)).thenReturn(Optional.of(inventory));

    AekoReportOutputDTO response = new AekoReportOutputDTO(
        "Inventário 2025", "2025-01-01", "2025-12-31",
        List.of(emission(1, false), emission(1, true))
    );

    persistence.saveResult(1, response);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<EmissionEntity>> emissions = ArgumentCaptor.forClass(List.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ReductionEntity>> reductions = ArgumentCaptor.forClass(List.class);

    verify(emissionRepository).saveAll(emissions.capture());
    verify(reductionRepository).saveAll(reductions.capture());

    assertEquals(1, emissions.getValue().size());
    assertEquals(1, reductions.getValue().size());
    assertEquals(InventoryStatusEnum.UNDER_REVIEW, inventory.getStatus());
    assertEquals("Inventário 2025", inventory.getDescription());
    assertEquals(LocalDate.of(2025, 1, 1), inventory.getInventoryPeriodStart());
    assertEquals(LocalDate.of(2025, 12, 31), inventory.getInventoryPeriodEnd());
    verify(inventoryRepository).save(inventory);
  }

  @Test
  @DisplayName("Should discard an inconsistent period returned by the AI")
  void discardsInvalidPeriod() {
    stubCatalogs();
    when(inventoryRepository.findByIdForUpdate(1)).thenReturn(Optional.of(inventory));

    persistence.saveResult(1, new AekoReportOutputDTO(
        "desc", "2025-12-31", "2025-01-01", List.of(emission(1, false))
    ));

    assertNull(inventory.getInventoryPeriodStart());
    assertNull(inventory.getInventoryPeriodEnd());
    assertEquals(InventoryStatusEnum.UNDER_REVIEW, inventory.getStatus());
  }

  @Test
  @DisplayName("Should fail (retryable) and save nothing when the AI returns an unknown gas")
  void unknownGas() {
    stubCatalogs();
    when(inventoryRepository.findByIdForUpdate(1)).thenReturn(Optional.of(inventory));

    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> persistence.saveResult(1, new AekoReportOutputDTO(
            "desc", null, null, List.of(emission(99, false))
        ))
    );

    assertTrue(ex.isRetryable());
    verify(emissionRepository, never()).saveAll(any());
    assertEquals(InventoryStatusEnum.PROCESSING, inventory.getStatus());
  }

  @Test
  @DisplayName("Should ignore the result when the inventory is no longer PROCESSING")
  void ignoresWhenNotProcessing() {
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);
    when(inventoryRepository.findByIdForUpdate(1)).thenReturn(Optional.of(inventory));

    persistence.saveResult(1, new AekoReportOutputDTO(
        "desc", null, null, List.of(emission(1, false))
    ));

    verify(emissionRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("Should mark the inventory as PROCESSING_FAILED with a truncated message")
  void markFailed() {
    when(inventoryRepository.findByIdForUpdate(1)).thenReturn(Optional.of(inventory));

    persistence.markFailed(1, "x".repeat(300));

    assertEquals(InventoryStatusEnum.PROCESSING_FAILED, inventory.getStatus());
    verify(inventoryRepository).save(inventory);
  }
}
