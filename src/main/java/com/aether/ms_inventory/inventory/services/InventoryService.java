package com.aether.ms_inventory.inventory.services;

import com.aether.ms_inventory.inventory.dto.input.*;
import com.aether.ms_inventory.inventory.dto.output.*;
import com.aether.ms_inventory.inventory.mappers.InventoryMapper;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.exceptions.NotFoundException;
import com.aether.ms_inventory.shared.persistence.postgres.entities.DepartmentEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.StorageFileRepository;
import com.aether.ms_inventory.shared.persistence.postgres.specifications.InventorySpecification;
import com.aether.ms_inventory.shared.persistence.redis.entities.InputInventoryDocument;
import com.aether.ms_inventory.shared.persistence.redis.repositories.InputInventoryRepository;
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
  private final InputInventoryRepository inputInventoryRepository;
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

    inventory.setConsolidationApproach(input.consolidationApproach());

    inventoryRepository.save(inventory);

    InputInventoryDocument inputInventory = new InputInventoryDocument(
        inventory.getId(),
        inventory.getStatus()
    );

    inputInventoryRepository.save(inputInventory);

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

  @Transactional(readOnly = true)
  public FindManyPendingInventoriesOutputDTO findManyPendingReports(
      FindManyPendingInventoriesInputDTO input
  ){
    DepartmentEntity department = getUserInfosService.getDepartment(input.userId());

    Specification<InventoryEntity> spec = Specification
        .where(InventorySpecification.withName(input.name()))
        .and(InventorySpecification.withNotOwnerEmployeeId(input.userId()))
        .and(InventorySpecification.withStatus(InventoryStatusEnum.UNDER_REVIEW))
        .and(InventorySpecification.withDepartment(department.getId()));

    List<InventoryEntity> inventories = inventoryRepository.findWithPagination(
        spec,
        input.skip(),
        input.take()
    );

    long count = inventoryRepository.count(spec);

    return InventoryMapper.convertFindManyPendingEntitiesToOutput(inventories, count);
  }

  @Transactional(readOnly = true)
  public FindManyReviewedInventoriesOutputDTO findManyReviewedInventories(
      FindManyReviewedInventoriesInputDTO input
  ){
    DepartmentEntity department = getUserInfosService.getDepartment(input.userId());

    Specification<InventoryEntity> spec = Specification
        .where(InventorySpecification.withName(input.name()))
        .and(InventorySpecification.withStatus(input.status()))
        .and(InventorySpecification.withNotProcessing())
        .and(InventorySpecification.withDepartment(department.getId()));

    List<InventoryEntity> inventories = inventoryRepository.findWithPagination(
        spec,
        input.skip(),
        input.take()
    );

    long count = inventoryRepository.count(spec);

    return InventoryMapper.convertFindManyReviewedEntitiesToOutput(inventories, count);
  }

  public RejectInventoryOutputDTO rejectInventory(
      RejectInventoryInputDTO input
  ){
    DepartmentEntity department = getUserInfosService.getDepartment(input.validatorId());

    InventoryEntity inventory = inventoryRepository.findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(input.id(), department.getId(), InventoryStatusEnum.UNDER_REVIEW, input.validatorId()).orElseThrow(
        () -> new NotFoundException("exception.inventory.not-found")
    );

    inventory.setStatus(InventoryStatusEnum.REJECTED);
    inventory.setStatusMessage(input.message());
    inventoryRepository.save(inventory);

    return InventoryMapper.convertRejectInventoryEntityToOutput(inventory);
  }

  public ApproveInventoryOutputDTO approveInventory(
      ApproveInventoryInputDTO input
  ){
    DepartmentEntity department = getUserInfosService.getDepartment(input.validatorId());

    InventoryEntity inventory = inventoryRepository.findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(input.id(), department.getId(), InventoryStatusEnum.UNDER_REVIEW, input.validatorId()).orElseThrow(
        () -> new NotFoundException("exception.inventory.not-found")
    );

    inventory.setStatus(InventoryStatusEnum.APPROVED);
    inventory.setStatusMessage(input.message());
    inventoryRepository.save(inventory);

    return InventoryMapper.convertApproveInventoryEntityToOutput(inventory);
  }

  @Transactional(readOnly = true)
  public LastInventoryOutputDTO findMyLastInventory(
      FindLastInventoryInputDTO input
  ){
    Specification<InventoryEntity> spec = Specification
        .where(InventorySpecification.withOwnerEmployeeId(input.userId()))
        .and(InventorySpecification.withType(InventoryTypeEnum.INPUT));

    InventoryEntity inventory = inventoryRepository.findWithPagination(spec, 0, 1)
        .stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("exception.last-mine-inventory.not-found"));

    return InventoryMapper.convertLastInventoryEntityToOutput(inventory);
  }

  @Transactional(readOnly = true)
  public LastInventoryOutputDTO findReviewedLastInventory(
      FindLastInventoryInputDTO input
  ){
    Specification<InventoryEntity> spec = Specification
        .where(InventorySpecification.withValidatorEmployeeId(input.userId()))
        .and(InventorySpecification.withNotProcessing());

    InventoryEntity inventory = inventoryRepository.findWithPagination(spec, 0, 1)
        .stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("exception.last-reviewed-inventory.not-found"));

    return InventoryMapper.convertLastInventoryEntityToOutput(inventory);
  }

  @Transactional(readOnly = true)
  public FindInventoryByIdOutputDTO findMineInventoryById(
      FindInventoryByIdInputDTO input
  ){
    InventoryEntity inventory = inventoryRepository.findByIdAndOwnerEmployeeId(input.id(), input.userId()).orElseThrow(
        () -> new NotFoundException("exception.inventory.not-found")
    );

    return InventoryMapper.convertFindInventoryByIdEntityToOutput(inventory);
  }

  @Transactional(readOnly = true)
  public FindInventoryByIdOutputDTO findReviewedInventoryById(
      FindInventoryByIdInputDTO input
  ){
    DepartmentEntity department = getUserInfosService.getDepartment(input.userId());

    InventoryEntity inventory = inventoryRepository.findByIdAndDepartmentId(input.id(), department.getId()).orElseThrow(
        () -> new NotFoundException("exception.inventory.not-found")
    );

    return InventoryMapper.convertFindInventoryByIdEntityToOutput(inventory);
  }
}
