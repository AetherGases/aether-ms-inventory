package com.aether.ms_inventory.shared.persistence.postgres.repositories;

import com.aether.ms_inventory.shared.enums.EmployeeStatusEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Integer> {
  Optional<EmployeeEntity> findByEmailAndStatus(
      String email,
      EmployeeStatusEnum status
  );

  Optional<EmployeeEntity> findByIdAndStatus(
      Integer id,
      EmployeeStatusEnum status
  );
  Optional<EmployeeEntity> findByEmail(
      String email
  );

}
