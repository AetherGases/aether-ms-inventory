package com.aether.ms_inventory.shared.services;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.AddressEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.DepartmentEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EnterpriseEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.ReductionEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.UnitEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.custom.GhgDeclarationRepository;
import com.aether.ms_inventory.shared.persistence.postgres.specifications.InventorySpecification;
import com.aether.ms_inventory.shared.workers.contexts.InventoryOutputContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryGenerationPersistence {
  private final InventoryRepository inventoryRepository;
  private final GhgDeclarationRepository ghgDeclarationRepository;

  @Transactional
  public boolean markProcessing(Integer inventoryId) {
    Optional<InventoryEntity> found = inventoryRepository.findById(inventoryId);

    if (found.isEmpty() || found.get().getStatus() != InventoryStatusEnum.REQUESTED) {
      return false;
    }

    InventoryEntity inventory = found.get();
    inventory.setStatus(InventoryStatusEnum.PROCESSING);
    inventoryRepository.save(inventory);

    return true;
  }

  @Transactional(readOnly = true)
  public Optional<InventoryOutputContext> loadContext(Integer inventoryId) {
    Optional<InventoryEntity> found = inventoryRepository.findById(inventoryId);

    if (found.isEmpty() || found.get().getStatus() != InventoryStatusEnum.PROCESSING) {
      return Optional.empty();
    }

    InventoryEntity inventory = found.get();

    DepartmentEntity department = inventory.getDepartment();
    UnitEntity ownerUnit = department == null ? null : department.getUnit();
    EnterpriseEntity enterprise = ownerUnit == null ? null : ownerUnit.getEnterprise();
    EmployeeEntity responsible = inventory.getOwnerEmployee();

    List<InventoryEntity> departmentInputInventories = department == null
        ? List.of() : findDepartmentInputInventories(department.getId());

    return Optional.of(new InventoryOutputContext(
        new InventoryOutputContext.InventoryData(
            inventory.getId(),
            inventory.getName(),
            inventory.getDescription(),
            inventory.getConsolidationApproach(),
            inventory.getInventoryPeriodStart(),
            inventory.getInventoryPeriodEnd()
        ),
        enterprise == null ? null : toEnterpriseData(enterprise, ownerUnit),
        responsible == null ? null : new InventoryOutputContext.ResponsibleData(
            responsible.getName(),
            null,
            responsible.getPhone(),
            null,
            responsible.getEmail()
        ),
        enterprise == null || enterprise.getUnits() == null ? List.of() : enterprise.getUnits().stream()
            .map(unit -> toUnitData(unit, enterprise))
            .toList(),
        enterprise == null ? List.of() : ghgDeclarationRepository.findCompleteDeclaration(enterprise.getId()),
        departmentInputInventories.stream()
            .filter(inputInventory -> inputInventory.getReductions() != null)
            .flatMap(inputInventory -> inputInventory.getReductions().stream())
            .map(InventoryGenerationPersistence::toReductionData)
            .toList()
    ));
  }

  private List<InventoryEntity> findDepartmentInputInventories(Integer departmentId) {
    Specification<InventoryEntity> spec = Specification
        .where(InventorySpecification.withDepartment(departmentId))
        .and(InventorySpecification.withType(InventoryTypeEnum.INPUT))
        .and(InventorySpecification.withNotProcessing());

    return inventoryRepository.findAll(spec);
  }

  @Transactional
  public void markIssued(Integer inventoryId, StorageFileEntity storageFile) {
    inventoryRepository.findById(inventoryId).ifPresent(inventory -> {
      if (inventory.getStatus() != InventoryStatusEnum.PROCESSING) {
        return;
      }

      inventory.setStatus(InventoryStatusEnum.ISSUED);
      inventory.setStorageFile(storageFile);
      inventoryRepository.save(inventory);
    });
  }

  @Transactional
  public void markFailed(Integer inventoryId, String message) {
    inventoryRepository.findById(inventoryId).ifPresent(inventory -> {
      if (inventory.getStatus() != InventoryStatusEnum.PROCESSING) {
        return;
      }

      inventory.setStatus(InventoryStatusEnum.PROCESSING_FAILED);
      inventory.setStatusMessage(message);
      inventoryRepository.save(inventory);
    });
  }

  private static InventoryOutputContext.EnterpriseData toEnterpriseData(
      EnterpriseEntity enterprise,
      UnitEntity ownerUnit
  ) {
    return new InventoryOutputContext.EnterpriseData(
        enterprise.getName(),
        enterprise.getTradeName(),
        enterprise.getCnpj(),
        toAddressData(enterprise.getAddress()),
        ownerUnit.getCnae(),
        null,
        null,
        null,
        null
    );
  }

  private static InventoryOutputContext.UnitData toUnitData(UnitEntity unit, EnterpriseEntity enterprise) {
    AddressEntity address = unit.getAddress();

    return new InventoryOutputContext.UnitData(
        unit.getCnpj(),
        enterprise.getName(),
        formatFullAddress(address),
        address == null ? null : address.getCity(),
        false,
        null
    );
  }

  private static InventoryOutputContext.AddressData toAddressData(AddressEntity address) {
    if (address == null) {
      return null;
    }

    return new InventoryOutputContext.AddressData(
        address.getZipCode(),
        address.getState(),
        address.getCity(),
        address.getNeighborhood(),
        address.getStreet(),
        address.getNumber(),
        address.getComplement()
    );
  }

  private static String formatFullAddress(AddressEntity address) {
    if (address == null) {
      return null;
    }

    StringBuilder builder = new StringBuilder();

    appendPart(builder, address.getStreet());
    if (address.getNumber() != null) {
      appendPart(builder, address.getNumber().toString());
    }
    appendPart(builder, address.getNeighborhood());
    appendPart(builder, address.getCity());
    appendPart(builder, address.getState());

    return builder.isEmpty() ? null : builder.toString();
  }

  private static void appendPart(StringBuilder builder, String part) {
    if (part == null || part.isBlank()) {
      return;
    }

    if (!builder.isEmpty()) {
      builder.append(", ");
    }

    builder.append(part);
  }

  private static InventoryOutputContext.ReductionData toReductionData(ReductionEntity reduction) {
    return new InventoryOutputContext.ReductionData(
        reduction.getCategory() == null ? null : reduction.getCategory().getName(),
        reduction.getQuantityCo2e()
    );
  }
}
