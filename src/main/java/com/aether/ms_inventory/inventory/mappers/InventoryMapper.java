package com.aether.ms_inventory.inventory.mappers;

import com.aether.ms_inventory.inventory.dto.input.*;
import com.aether.ms_inventory.inventory.dto.output.*;
import com.aether.ms_inventory.inventory.dto.query_params.FindManyPendingInventoriesQueryParamsDTO;
import com.aether.ms_inventory.inventory.dto.query_params.FindManyReviewedInventoriesQueryParamsDTO;
import com.aether.ms_inventory.inventory.dto.query_params.FindMyInventoriesQueryParamsDTO;
import com.aether.ms_inventory.inventory.dto.request.RegisterInventoryRequestDTO;
import com.aether.ms_inventory.shared.AetherConstants;
import com.aether.ms_inventory.shared.helpers.NormalizeInput;
import com.aether.ms_inventory.shared.helpers.NormalizeOutput;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_inventory.inventory.dto.route_params.FindInventoryByIdRouteParamsDTO;
import java.util.List;

public class InventoryMapper {
  public static FindMyInventoriesHistoryInputDTO convertFindMyInventoriesQueryToInput(Integer userId, FindMyInventoriesQueryParamsDTO query){
    return new FindMyInventoriesHistoryInputDTO(
        userId,
        query.name(),
        query.status(),
        query.skip() == null ? 0 : query.skip(),
        query.take() == null ? AetherConstants.DEFAULT_TAKE : query.take()
    );
  }

  public static RegisterInventoryInputDTO convertRegisterInventoryRequestToInput(Integer userId, RegisterInventoryRequestDTO request){
    return new RegisterInventoryInputDTO(
        userId,
        NormalizeInput.name(request.name()),
        NormalizeInput.name(request.consolidationApproach()),
        NormalizeInput.name(request.fileName()),
        NormalizeInput.url(request.path())
    );
  }

  public static FindManyPendingInventoriesInputDTO convertFindManyPendingInventoryQueryToInput(Integer userId, FindManyPendingInventoriesQueryParamsDTO query){
    return new FindManyPendingInventoriesInputDTO(
        userId,
        query.name(),
        query.skip() == null ? 0 : query.skip(),
        query.take() == null ? AetherConstants.DEFAULT_TAKE : query.take()
    );
  }

  public static FindMyInventoriesHistoryOutputDTO convertFindMyInventoriesToOutput(List<InventoryEntity> inventories, long count){
    return new FindMyInventoriesHistoryOutputDTO(
        inventories.stream().map(inventory -> new FindMyInventoriesHistoryOutputDTO.Inventory(
            inventory.getId(),
            NormalizeOutput.name(inventory.getName()),
            inventory.getCreatedAt(),
            inventory.getStatus(),
            inventory.getType()
        )).toList(),
        count
    );
  }

  public static RegisterInventoryOutputDTO convertRegisterInventoryEntitiesToOutput(InventoryEntity inventory, StorageFileEntity storageFile){
    return new RegisterInventoryOutputDTO(
        inventory.getId(),
        NormalizeOutput.name(inventory.getName()),
        inventory.getType(),
        inventory.getStatus(),
        inventory.getCreatedAt(),
        new RegisterInventoryOutputDTO.StorageFile(
            storageFile.getId(),
            storageFile.getName(),
            NormalizeOutput.url(storageFile.getPath()),
            storageFile.getCreatedAt()
        )
    );
  }

  public static FindManyReviewedInventoriesInputDTO convertFindManyReviewedInventoryQueryToInput(Integer userId, FindManyReviewedInventoriesQueryParamsDTO query){
    return new FindManyReviewedInventoriesInputDTO(
        userId,
        query.name(),
        query.status(),
        query.skip() == null ? 0 : query.skip(),
        query.take() == null ? AetherConstants.DEFAULT_TAKE : query.take()
    );
  }

  public static FindManyPendingInventoriesOutputDTO convertFindManyPendingEntitiesToOutput(List<InventoryEntity> inventories, long count){
    return new FindManyPendingInventoriesOutputDTO(
        inventories.stream().map(inventory -> {
          EmployeeEntity author = inventory.getOwnerEmployee();

          return new FindManyPendingInventoriesOutputDTO.Inventory(
              inventory.getId(),
              NormalizeOutput.name(inventory.getName()),
              inventory.getCreatedAt(),
              NormalizeOutput.name(author.getName()),
              author.getStorageFile() != null ? NormalizeOutput.url(author.getStorageFile().getPath()) : null,
              inventory.getStatus(),
              inventory.getType()
          );
        }).toList(),
        count
    );
  }

  public static FindManyReviewedInventoriesOutputDTO convertFindManyReviewedEntitiesToOutput(List<InventoryEntity> inventories, long count){
    return new FindManyReviewedInventoriesOutputDTO(
        inventories.stream().map(inventory -> {
          EmployeeEntity author = inventory.getOwnerEmployee();

          return new FindManyReviewedInventoriesOutputDTO.Inventory(
              inventory.getId(),
              NormalizeOutput.name(inventory.getName()),
              inventory.getCreatedAt(),
              NormalizeOutput.name(author.getName()),
              author.getStorageFile() != null ? NormalizeOutput.url(author.getStorageFile().getPath()) : null,
              inventory.getStatus(),
              inventory.getType()
          );
        }).toList(),
        count
    );
  }


  public static RejectInventoryOutputDTO convertRejectInventoryEntityToOutput(InventoryEntity inventory){
    return new RejectInventoryOutputDTO(
        inventory.getId(),
        NormalizeOutput.name(inventory.getName()),
        inventory.getDescription(),
        inventory.getType(),
        inventory.getStatus(),
        inventory.getStatusMessage(),
        inventory.getCreatedAt(),
        inventory.getUpdatedAt()
    );
  }

  public static ApproveInventoryOutputDTO convertApproveInventoryEntityToOutput(InventoryEntity inventory){
    return new ApproveInventoryOutputDTO(
        inventory.getId(),
        NormalizeOutput.name(inventory.getName()),
        inventory.getDescription(),
        inventory.getType(),
        inventory.getStatus(),
        inventory.getStatusMessage(),
        inventory.getCreatedAt(),
        inventory.getUpdatedAt()
    );
  }

  public static FindInventoryByIdInputDTO convertFindInventoryByIdRouteParamsToInput(Integer userId, FindInventoryByIdRouteParamsDTO routeParams){
    return new FindInventoryByIdInputDTO(
        routeParams.id(),
        userId
    );
  }

  public static FindInventoryByIdOutputDTO convertFindInventoryByIdEntityToOutput(InventoryEntity inventory){
    StorageFileEntity storageFile = inventory.getStorageFile();

    return new FindInventoryByIdOutputDTO(
        inventory.getId(),
        NormalizeOutput.name(inventory.getName()),
        NormalizeOutput.description(inventory.getDescription()),
        inventory.getConsolidationApproach(),
        inventory.getInventoryPeriodStart(),
        inventory.getInventoryPeriodEnd(),
        inventory.getType(),
        inventory.getStatus(),
        inventory.getStatusMessage(),
        inventory.getCreatedAt(),
        inventory.getUpdatedAt(),
        convertEmployeeToOutput(inventory.getOwnerEmployee()),
        convertEmployeeToOutput(inventory.getValidatorEmployee()),
        storageFile == null ? null : new FindInventoryByIdOutputDTO.StorageFile(
            storageFile.getId(),
            storageFile.getName(),
            NormalizeOutput.url(storageFile.getPath()),
            storageFile.getCreatedAt()
        )
    );
  }

  public static LastInventoryOutputDTO convertLastInventoryEntityToOutput(InventoryEntity inventory){
    EmployeeEntity author = inventory.getOwnerEmployee();

    return new LastInventoryOutputDTO(
        inventory.getId(),
        NormalizeOutput.name(inventory.getName()),
        new LastInventoryOutputDTO.Author(
            NormalizeOutput.name(author.getName()),
            author.getStorageFile() != null ? NormalizeOutput.url(author.getStorageFile().getPath()) : null
        ),
        new LastInventoryOutputDTO.Period(
            inventory.getInventoryPeriodStart(),
            inventory.getInventoryPeriodEnd()
        ),
        inventory.getStatus(),
        inventory.getType(),
        inventory.getCreatedAt(),
        inventory.getUpdatedAt()
    );
  }

  private static FindInventoryByIdOutputDTO.Employee convertEmployeeToOutput(EmployeeEntity employee){
    if (employee == null) return null;

    return new FindInventoryByIdOutputDTO.Employee(
        employee.getId(),
        NormalizeOutput.name(employee.getName()),
        employee.getStorageFile() != null ? NormalizeOutput.url(employee.getStorageFile().getPath()) : null
    );
  }
}
