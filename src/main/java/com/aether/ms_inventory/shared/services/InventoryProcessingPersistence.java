package com.aether.ms_inventory.shared.services;

import com.aether.ms_inventory.shared.dto.output.AekoReportOutputDTO;
import com.aether.ms_inventory.shared.dto.request.AekoReportRequestDTO;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.exceptions.InventoryProcessingException;
import com.aether.ms_inventory.shared.persistence.postgres.entities.CategoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmissionEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.GasEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.ReductionEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.ScopeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.CategoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.EmissionRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.GasRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.ReductionRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.ScopeRepository;
import com.aether.ms_inventory.shared.workers.InventoryProcessingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryProcessingPersistence {
  private static final int MAX_DESCRIPTION = 150;
  private static final int MAX_METHODOLOGY = 150;
  private static final int MAX_STATUS_MESSAGE = 100;

  private final InventoryRepository inventoryRepository;
  private final EmissionRepository emissionRepository;
  private final ReductionRepository reductionRepository;
  private final GasRepository gasRepository;
  private final ScopeRepository scopeRepository;
  private final CategoryRepository categoryRepository;
  
  @Transactional(readOnly = true)
  public Optional<InventoryProcessingContext> loadContext(Integer inventoryId) {
    Optional<InventoryEntity> found = inventoryRepository.findById(inventoryId);

    if (found.isEmpty() || found.get().getStatus() != InventoryStatusEnum.PROCESSING) {
      return Optional.empty();
    }

    InventoryEntity inventory = found.get();

    StorageFileEntity file = inventory.getStorageFile();
    if (file == null || file.getPath() == null)
      throw new InventoryProcessingException("Inventário sem arquivo associado", false);

    if (inventory.getOwnerEmployee() == null)
      throw new InventoryProcessingException("Inventário sem usuário responsável", false);

    List<AekoReportRequestDTO.Gas> gases = gasRepository.findAll().stream()
        .map(gas -> new AekoReportRequestDTO.Gas(gas.getId(), gas.getFormula()))
        .toList();

    List<AekoReportRequestDTO.Scope> scopes = scopeRepository.findAll().stream()
        .map(scope -> new AekoReportRequestDTO.Scope(scope.getId(), scope.getName()))
        .toList();

    List<AekoReportRequestDTO.Category> categories = categoryRepository.findAll().stream()
        .map(category -> new AekoReportRequestDTO.Category(
            category.getId(),
            category.getName(),
            category.getClassification() == null ? null : category.getClassification().name()
        ))
        .toList();

    return Optional.of(new InventoryProcessingContext(
        inventory.getId(),
        file.getPath(),
        inventory.getOwnerEmployee().getId(),
        gases,
        scopes,
        categories
    ));
  }

  @Transactional
  public void saveResult(Integer inventoryId, AekoReportOutputDTO response) {
    InventoryEntity inventory = inventoryRepository.findByIdForUpdate(inventoryId)
        .orElseThrow(() -> new InventoryProcessingException("Inventário não encontrado", false));

    if (inventory.getStatus() != InventoryStatusEnum.PROCESSING) {
      log.warn("Inventário {} já não está em PROCESSING, resultado da IA descartado", inventoryId);
      return;
    }

    if (response == null || response.emissions() == null || response.emissions().isEmpty()) {
      throw new InventoryProcessingException("A IA não retornou emissões para o inventário", true);
    }

    Map<Integer, GasEntity> gases = indexById(gasRepository.findAll(), GasEntity::getId);
    Map<Integer, ScopeEntity> scopes = indexById(scopeRepository.findAll(), ScopeEntity::getId);
    Map<Integer, CategoryEntity> categories =
        indexById(categoryRepository.findAll(), CategoryEntity::getId);

    List<EmissionEntity> emissions = new ArrayList<>();
    List<ReductionEntity> reductions = new ArrayList<>();

    for (AekoReportOutputDTO.Emission item : response.emissions()) {
      if (item == null || item.quantityCo2e() == null) {
        throw new InventoryProcessingException("A IA retornou uma emissão sem quantidade", true);
      }

      CategoryEntity category = lookup(categories, item.category(), "categoria", false);

      if (Boolean.TRUE.equals(item.reduction())) {
        ReductionEntity reduction = new ReductionEntity();
        reduction.setQuantityCo2e(item.quantityCo2e());
        reduction.setCategory(category);
        reduction.setInventory(inventory);
        reductions.add(reduction);
        continue;
      }

      if (item.quantityCo2e().signum() < 0) {
        throw new InventoryProcessingException("A IA retornou uma emissão negativa", true);
      }

      BigDecimal supplierPercentage = item.supplierDataPercentage();
      if (supplierPercentage != null
          && (supplierPercentage.signum() < 0
          || supplierPercentage.compareTo(BigDecimal.valueOf(100)) > 0)) {
        throw new InventoryProcessingException(
            "A IA retornou percentual de dados do fornecedor fora de 0-100", true
        );
      }

      EmissionEntity emission = new EmissionEntity();
      emission.setQuantityCo2e(item.quantityCo2e());
      emission.setMethodologyDescription(truncate(item.methodologyDescription(), MAX_METHODOLOGY));
      emission.setSupplierDataPercentage(supplierPercentage);
      emission.setGas(lookup(gases, item.gas(), "gás", true));
      emission.setScope(lookup(scopes, item.scope(), "escopo", true));
      emission.setCategory(category);
      emission.setInventory(inventory);
      emissions.add(emission);
    }

    emissionRepository.saveAll(emissions);
    reductionRepository.saveAll(reductions);

    LocalDate start = parseDate(response.startPeriod());
    LocalDate end = parseDate(response.endPeriod());

    if (start != null && end != null && end.isBefore(start)) {
      start = null;
      end = null;
    }

    inventory.setDescription(truncate(response.description(), MAX_DESCRIPTION));
    inventory.setInventoryPeriodStart(start);
    inventory.setInventoryPeriodEnd(end);
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);
    inventory.setUpdatedAt(LocalDateTime.now());

    inventoryRepository.save(inventory);
  }

  @Transactional
  public void markFailed(Integer inventoryId, String message) {
    inventoryRepository.findByIdForUpdate(inventoryId).ifPresent(inventory -> {
      if (inventory.getStatus() != InventoryStatusEnum.PROCESSING) return;

      inventory.setStatus(InventoryStatusEnum.PROCESSING_FAILED);
      inventory.setUpdatedAt(LocalDateTime.now());

      inventoryRepository.save(inventory);
    });
  }

  private static <T> Map<Integer, T> indexById(List<T> items, Function<T, Integer> idOf) {
    return items.stream().collect(Collectors.toMap(idOf, Function.identity()));
  }

  private static <T> T lookup(Map<Integer, T> items, Integer id, String label, boolean required) {
    if (id == null) {
      if (required) {
        throw new InventoryProcessingException("A IA retornou uma emissão sem " + label, true);
      }

      return null;
    }

    T item = items.get(id);

    if (item == null) {
      throw new InventoryProcessingException(
          "A IA retornou " + label + " inexistente (id " + id + ")", true
      );
    }

    return item;
  }

  private static LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return LocalDate.parse(value.trim());
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return null;
    }

    String trimmed = value.trim();

    return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
  }
}
