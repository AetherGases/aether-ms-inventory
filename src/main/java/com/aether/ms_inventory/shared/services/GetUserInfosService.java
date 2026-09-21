package com.aether.ms_inventory.shared.services;

import com.aether.ms_inventory.shared.enums.EmployeeStatusEnum;
import com.aether.ms_inventory.shared.exceptions.UnauthorizedException;
import com.aether.ms_inventory.shared.persistence.postgres.entities.DepartmentEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.EmployeeRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetUserInfosService {
  private final EmployeeRepository employeeRepository;

  @Transactional(readOnly = true)
  public DepartmentEntity getDepartment(Integer employeeId){
    EmployeeEntity employee = employeeRepository.findByIdAndStatus(employeeId, EmployeeStatusEnum.ACTIVE).orElseThrow(
        () -> new UnauthorizedException("exception.login.invalid")
    );

    return employee.getDepartment();
  }

  public EmployeeEntity getEmployee(Integer employeeId){
    EmployeeEntity employee = employeeRepository.findByIdAndStatus(employeeId, EmployeeStatusEnum.ACTIVE).orElseThrow(
        () -> new UnauthorizedException("exception.login.invalid")
    );

    return employee;
  }
}
