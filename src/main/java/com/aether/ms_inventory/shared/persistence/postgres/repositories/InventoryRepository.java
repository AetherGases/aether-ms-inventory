package com.aether.ms_inventory.shared.persistence.postgres.repositories;

import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.custom.interfaces.InventoryRepositoryCustom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, Integer>, JpaSpecificationExecutor<InventoryEntity>, InventoryRepositoryCustom {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM InventoryEntity i WHERE i.id = :id")
  Optional<InventoryEntity> findByIdForUpdate(@Param("id") Integer id);
}
