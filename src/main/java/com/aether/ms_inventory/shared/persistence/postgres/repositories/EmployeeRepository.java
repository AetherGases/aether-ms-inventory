package com.aether.ms_inventory.shared.persistence.postgres.repositories;

import com.aether.ms_inventory.shared.enums.EmployeeStatusEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

  /**
   * Aprovadores de um departamento: funcionarios ACTIVE do departamento, exceto o
   * dono do inventario, cujo grupo de permissao possui uma permissao cujo padrao
   * (regex) libera a URI de aprovacao informada. Segue a mesma regra de
   * PermissionAuthorizationManager (match completo do padrao contra a URI).
   */
  @Query(value = """
      SELECT DISTINCT e.*
      FROM employee e
      JOIN permission_group_permission pgp ON pgp.id_permission_group = e.id_permission_group
      JOIN permission p ON p.id = pgp.id_permission
      WHERE e.id_department = :departmentId
        AND e.employee_status = 'ACTIVE'
        AND e.id <> :ownerEmployeeId
        AND :approveUri ~ ('^(?:' || p.pattern || ')$')
      """, nativeQuery = true)
  List<EmployeeEntity> findApproversByDepartment(
      @Param("departmentId") Integer departmentId,
      @Param("ownerEmployeeId") Integer ownerEmployeeId,
      @Param("approveUri") String approveUri
  );
}
