package com.aether.ms_inventory.shared.persistence.postgres.repositories.custom.interfaces;

import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface InventoryRepositoryCustom {
  List<InventoryEntity> findWithPagination(
      Specification<InventoryEntity> specification,
      Integer skip,
      Integer take
  );
}
