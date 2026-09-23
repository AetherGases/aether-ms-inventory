package com.aether.ms_inventory.inventory.services;

import com.aether.ms_inventory.inventory.dto.input.FindMyInventoriesHistoryInputDTO;
import com.aether.ms_inventory.inventory.dto.input.RegisterInventoryInputDTO;
import com.aether.ms_inventory.inventory.dto.output.FindMyInventoriesHistoryOutputDTO;
import com.aether.ms_inventory.inventory.dto.output.RegisterInventoryOutputDTO;
import com.aether.ms_inventory.inventory.mappers.InventoryMapper;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.DepartmentEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.StorageFileRepository;
import com.aether.ms_inventory.shared.persistence.postgres.specifications.InventorySpecification;
import com.aether.ms_inventory.shared.services.GetUserInfosService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {
  private final InventoryRepository inventoryRepository;
  private final StorageFileRepository storageFileRepository;
  private final GetUserInfosService getUserInfosService;

  @Transactional
  public RegisterInventoryOutputDTO registerInventory(RegisterInventoryInputDTO input){
    EmployeeEntity employee = getUserInfosService.getEmployee(input.userId());

    StorageFileEntity storageFile = new StorageFileEntity(
        input.fileName(),
        input.path()
    );

    storageFileRepository.save(storageFile);

    InventoryEntity inventory = new InventoryEntity(
        input.name(),
        InventoryTypeEnum.INPUT,
        employee,
        storageFile
    );

    inventoryRepository.save(inventory);

    return InventoryMapper.convertRegisterInventoryEntitiesToOutput(
        inventory,
        storageFile
    );
  }

  public FindMyInventoriesHistoryOutputDTO findMyInventoriesHistory(
      FindMyInventoriesHistoryInputDTO input
  ) {
    DepartmentEntity department = getUserInfosService.getDepartment(input.userId());

    Specification<InventoryEntity> spec = Specification
        .where(InventorySpecification.withName(input.name()))
        .and(InventorySpecification.withStatus(input.status()))
        .and(InventorySpecification.withDepartment(department.getId()))
        .and(InventorySpecification.withOwnerEmployeeId(input.userId()));

    List<InventoryEntity> inventories = inventoryRepository.findWithPagination(
        spec,
        input.skip(),
        input.take()
    );

    long count = inventoryRepository.count(spec);

    return InventoryMapper.convertFindMyInventoriesToOutput(
        inventories,
        count
    );
  }
}
