package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import com.aether.ms_inventory.shared.enums.EmployeeStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "employee")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EmployeeEntity extends UserBaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = AetherConstants.CPF_LENGTH, nullable = false, unique = true)
  @JdbcTypeCode(SqlTypes.CHAR)
  private String cpf;

  @Column(length = AetherConstants.MAX_EMPLOYEE_NAME_LENGTH)
  private String name;

  @Column(length = AetherConstants.MAX_EMPLOYEE_PHONE_LENGTH)
  private String phone;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "employee_status", nullable = false, columnDefinition = "EMPLOYEE_STATUS")
  private EmployeeStatusEnum status;

  @ManyToMany
  @JoinTable(
      name = "permission_group_employee",
      joinColumns = @JoinColumn(name = "id_employee"),
      inverseJoinColumns = @JoinColumn(name = "id_permission_group")
  )
  private List<PermissionGroupEntity> permissionGroups;

  @ManyToOne
  @JoinColumn(name = "id_department")
  private DepartmentEntity department;

  @OneToOne
  @JoinColumn(name = "id_storage_file")
  private StorageFileEntity storageFile;

  @OneToMany(mappedBy = "ownerEmployee")
  private List<InventoryEntity> inventories;

  @OneToMany(mappedBy = "validatorEmployee")
  private List<InventoryEntity> validateInventories;

  public EmployeeEntity(String cpf, String name, String email, String passwordHash, String phone) {
    this.cpf = cpf;
    this.name = name;
    this.setEmail(email);
    this.setPasswordHash(passwordHash);
    this.phone = phone;
    this.status = EmployeeStatusEnum.ACTIVE;
  }

  public EmployeeEntity(String cpf, String name, String email, String phone, EmployeeStatusEnum status, List<PermissionGroupEntity> permissionGroups) {
    this.cpf = cpf;
    this.name = name;
    this.setEmail(email);
    this.phone = phone;
    this.status = status;
    this.permissionGroups = permissionGroups;
  }

  public EmployeeEntity(String cpf, String name, String email, String passwordHash, String phone, EmployeeStatusEnum status, List<PermissionGroupEntity> permissionGroups) {
    this.cpf = cpf;
    this.name = name;
    this.setEmail(email);
    this.setPasswordHash(passwordHash);
    this.phone = phone;
    this.status = status;
    this.permissionGroups = permissionGroups;
  }
}
