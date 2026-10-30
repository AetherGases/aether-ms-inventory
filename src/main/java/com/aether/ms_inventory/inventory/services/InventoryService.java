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
import com.aether.ms_inventory.shared.persistence.postgres.repositories.EmissionRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.ReductionRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.StorageFileRepository;
import com.aether.ms_inventory.shared.persistence.postgres.specifications.InventorySpecification;
import com.aether.ms_inventory.shared.persistence.redis.entities.InputInventoryDocument;
import com.aether.ms_inventory.shared.persistence.redis.repositories.InputInventoryRepository;
import com.aether.ms_inventory.shared.services.GetUserInfosService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {
  private final InventoryRepository inventoryRepository;
  private final StorageFileRepository storageFileRepository;
  private final InputInventoryRepository inputInventoryRepository;
  private final EmissionRepository emissionRepository;
  private final ReductionRepository reductionRepository;
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
        .and(InventorySpecification.withDepartment(department.getId()))
        .and(InventorySpecification.withType(InventoryTypeEnum.INPUT));

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
  public CountInventoriesOutputDTO countInventories(
      CountInventoriesInputDTO input
  ){
    DepartmentEntity department = getUserInfosService.getDepartment(input.userId());

    Specification<InventoryEntity> reviewedSpec = Specification
        .where(InventorySpecification.withValidatorEmployeeId(input.userId()))
        .and(InventorySpecification.withNotDepartment(department.getId()))
        .and(InventorySpecification.withReviewed());

    Specification<InventoryEntity> submittedSpec = Specification
        .where(InventorySpecification.withOwnerEmployeeId(input.userId()))
        .and(InventorySpecification.withType(InventoryTypeEnum.INPUT));

    long reviewedCount = inventoryRepository.count(reviewedSpec);
    long submittedCount = inventoryRepository.count(submittedSpec);

    return new CountInventoriesOutputDTO(reviewedCount, submittedCount);
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

  @Transactional(readOnly = true)
  public FindEnterpriseInfoOutputDTO findEnterpriseInfo(
      FindEnterpriseInfoInputDTO input
  ){
    DepartmentEntity department = getUserInfosService.getDepartment(input.userId());
    Integer enterpriseId = department.getUnit().getEnterprise().getId();

    BigDecimal totalReductions = roundToTwoDecimals(reductionRepository.sumQuantityCo2eByEnterpriseId(enterpriseId));
    BigDecimal totalEmissions = roundToTwoDecimals(emissionRepository.sumQuantityCo2eByEnterpriseId(enterpriseId));

    return new FindEnterpriseInfoOutputDTO(
        totalReductions,
        totalEmissions,
        calculateReducedPercentage(totalReductions, totalEmissions)
    );
  }

  @Transactional(readOnly = true)
  public FindUnitInfoOutputDTO findUnitInfo(
      FindUnitInfoInputDTO input
  ){
    DepartmentEntity department = getUserInfosService.getDepartment(input.userId());
    Integer unitId = department.getUnit().getId();

    BigDecimal totalReductions = roundToTwoDecimals(reductionRepository.sumQuantityCo2eByUnitId(unitId));
    BigDecimal totalEmissions = roundToTwoDecimals(emissionRepository.sumQuantityCo2eByUnitId(unitId));

    return new FindUnitInfoOutputDTO(
        totalReductions,
        totalEmissions,
        calculateReducedPercentage(totalReductions, totalEmissions)
    );
  }

  private BigDecimal calculateReducedPercentage(BigDecimal reductions, BigDecimal emissions) {
    if (emissions.signum() == 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    return reductions
        .multiply(BigDecimal.valueOf(100))
        .divide(emissions, 2, RoundingMode.HALF_UP);
  }

  private BigDecimal roundToTwoDecimals(BigDecimal value) {
    BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
    return safeValue.setScale(2, RoundingMode.HALF_UP);
  }
}
