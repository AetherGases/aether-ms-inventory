package com.aether.ms_inventory.inventory.mappers;

import com.aether.ms_inventory.inventory.dto.input.FindMyInventoriesHistoryInputDTO;
import com.aether.ms_inventory.inventory.dto.output.FindMyInventoriesHistoryOutputDTO;
import com.aether.ms_inventory.inventory.dto.query_params.FindMyInventoriesQueryParamsDTO;
import com.aether.ms_inventory.shared.AetherConstants;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;

import java.util.List;

public class InventoryMapper {
  public static FindMyInventoriesHistoryOutputDTO convertFindMyInventoriesToOutput(List<InventoryEntity> inventories, long count){
    return new FindMyInventoriesHistoryOutputDTO(
        inventories.stream().map(inventory -> new FindMyInventoriesHistoryOutputDTO.Inventory(
            inventory.getId(),
            inventory.getName(),
            inventory.getCreatedAt(),
            inventory.getStatus(),
            inventory.getType()
        )).toList(),
        count
    );
  }

  public static FindMyInventoriesHistoryInputDTO convertFindMyInventoriesQueryToInput(Integer userId, FindMyInventoriesQueryParamsDTO query){
    return new FindMyInventoriesHistoryInputDTO(
        userId,
        query.name(),
        query.status(),
        query.skip() == null ? 0 : query.skip(),
        query.take() == null ? AetherConstants.DEFAULT_TAKE : query.take()
    );
  }
}
