package com.aether.ms_inventory.shared.persistence.postgres.repositories;

import com.aether.ms_inventory.shared.persistence.postgres.entities.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, Integer> {
  PermissionEntity findByNameIgnoreCase(String name);

  @Query(
      "SELECT p FROM PermissionEntity p " +
      "JOIN p.permissionGroups pg " +
      "JOIN pg.employees e " +
      "WHERE e.id = :employeeId"
  )
  List<PermissionEntity> findAllByEmployeeId(Integer employeeId);
}
