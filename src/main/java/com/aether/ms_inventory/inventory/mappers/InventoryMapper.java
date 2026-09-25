package com.aether.ms_inventory.inventory.mappers;

import com.aether.ms_inventory.inventory.dto.input.FindManyPendingInventoriesInputDTO;
import com.aether.ms_inventory.inventory.dto.input.FindMyInventoriesHistoryInputDTO;
import com.aether.ms_inventory.inventory.dto.input.RegisterInventoryInputDTO;
import com.aether.ms_inventory.inventory.dto.output.FindManyPendingInventoriesOutputDTO;
import com.aether.ms_inventory.inventory.dto.output.FindMyInventoriesHistoryOutputDTO;
import com.aether.ms_inventory.inventory.dto.output.RegisterInventoryOutputDTO;
import com.aether.ms_inventory.inventory.dto.query_params.FindManyPendingInventoriesQueryParamsDTO;
import com.aether.ms_inventory.inventory.dto.query_params.FindMyInventoriesQueryParamsDTO;
import com.aether.ms_inventory.inventory.dto.request.RegisterInventoryRequestDTO;
import com.aether.ms_inventory.shared.AetherConstants;
import com.aether.ms_inventory.shared.helpers.NormalizeInput;
import com.aether.ms_inventory.shared.helpers.NormalizeOutput;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.StorageFileEntity;

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
            NormalizeOutput.name(storageFile.getName()),
            NormalizeOutput.url(storageFile.getPath()),
            storageFile.getCreatedAt()
        )
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
}
