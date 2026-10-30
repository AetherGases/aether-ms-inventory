package com.aether.ms_inventory.worker;

import com.aether.ms_inventory.shared.dto.output.AekoReportOutputDTO;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.exceptions.InventoryProcessingException;
import com.aether.ms_inventory.shared.persistence.postgres.entities.CategoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.GasEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    EmployeeEntity owner = new EmployeeEntity();
    owner.setId(7);
    inventory.setOwnerEmployee(owner);
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

  private AekoReportOutputDTO.Emission emission(Integer gas, Integer category, boolean reduction) {
    return new AekoReportOutputDTO.Emission(
        new BigDecimal("12.5"), "Fator GHG Protocol", new BigDecimal("40"),
        gas, 1, category, false, reduction
    );
  }

  @Test
  @DisplayName("Should register emissions and reductions through the procedures and move inventory to UNDER_REVIEW")
  void saveResult() {
    stubCatalogs();
    when(inventoryRepository.findByIdForUpdate(1)).thenReturn(Optional.of(inventory));

    AekoReportOutputDTO response = new AekoReportOutputDTO(
        "Inventário 2025", "2025-01-01", "2025-12-31",
        List.of(emission(1, false), emission(1, true))
    );

    persistence.saveResult(1, response);

    verify(emissionRepository).registerEmission(
        new BigDecimal("12.5"), 1, 1, 1, 2,
        "Fator GHG Protocol", new BigDecimal("40"),
        "7", "MS_INVENTORY_WORKER"
    );
    verify(reductionRepository).registerReduction(
        new BigDecimal("12.5"), 1, 2, "7", "MS_INVENTORY_WORKER"
    );
    verify(emissionRepository, never()).saveAll(any());
    verify(reductionRepository, never()).saveAll(any());

    assertEquals(InventoryStatusEnum.UNDER_REVIEW, inventory.getStatus());
    assertEquals("Inventário 2025", inventory.getDescription());
    assertEquals(LocalDate.of(2025, 1, 1), inventory.getInventoryPeriodStart());
    assertEquals(LocalDate.of(2025, 12, 31), inventory.getInventoryPeriodEnd());
    verify(inventoryRepository).save(inventory);
  }

  @Test
  @DisplayName("Should register an emission without category passing a null category to the procedure")
  void emissionWithoutCategory() {
    stubCatalogs();
    when(inventoryRepository.findByIdForUpdate(1)).thenReturn(Optional.of(inventory));

    persistence.saveResult(1, new AekoReportOutputDTO(
        "desc", null, null, List.of(emission(1, null, false), emission(1, null, false))
    ));

    verify(emissionRepository, times(2)).registerEmission(
        any(), eq(1), eq(1), eq(1), isNull(), any(), any(), eq("7"), eq("MS_INVENTORY_WORKER")
    );
  }

  @Test
  @DisplayName("Should fail (retryable) and register nothing when the AI returns a reduction without category")
  void reductionWithoutCategory() {
    stubCatalogs();
    when(inventoryRepository.findByIdForUpdate(1)).thenReturn(Optional.of(inventory));

    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> persistence.saveResult(1, new AekoReportOutputDTO(
            "desc", null, null, List.of(emission(1, false), emission(1, null, true))
        ))
    );

    assertTrue(ex.isRetryable());
    verify(emissionRepository, never()).registerEmission(any(), any(), any(), any(), any(), any(), any(), any(), any());
    verify(reductionRepository, never()).registerReduction(any(), any(), any(), any(), any());
    assertEquals(InventoryStatusEnum.PROCESSING, inventory.getStatus());
  }

  @Test
  @DisplayName("Should fail (retryable) and register nothing when the AI returns a negative reduction")
  void negativeReduction() {
    stubCatalogs();
    when(inventoryRepository.findByIdForUpdate(1)).thenReturn(Optional.of(inventory));

    AekoReportOutputDTO.Emission negative = new AekoReportOutputDTO.Emission(
        new BigDecimal("-3"), null, null, null, null, 2, false, true
    );

    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> persistence.saveResult(1, new AekoReportOutputDTO(
            "desc", null, null, List.of(negative)
        ))
    );

    assertTrue(ex.isRetryable());
    verify(reductionRepository, never()).registerReduction(any(), any(), any(), any(), any());
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
    verify(emissionRepository, never()).registerEmission(any(), any(), any(), any(), any(), any(), any(), any(), any());
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

    verify(emissionRepository, never()).registerEmission(any(), any(), any(), any(), any(), any(), any(), any(), any());
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
