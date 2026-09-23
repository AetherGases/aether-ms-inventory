package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "inventory")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class InventoryEntity extends DateBaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = AetherConstants.MAX_INVENTORY_NAME_LENGTH, nullable = false)
  private String name;

  @Column(length = AetherConstants.MAX_INVENTORY_DESCRIPTION_LENGTH)
  private String description;

  @Column(name = "consolidation_approach", length = AetherConstants.MAX_INVENTORY_CONSOLIDATION_APPROACH_LENGTH)
  private String consolidationApproach;

  @Column(name = "inventorying_period_start")
  private LocalDate inventoryPeriodStart;

  @Column(name = "inventorying_period_end")
  private LocalDate inventoryPeriodEnd;

  @ManyToOne
  @JoinColumn(name = "id_department")
  private DepartmentEntity department;

  @ManyToOne
  @JoinColumn(name = "id_owner_employee")
  private EmployeeEntity ownerEmployee;

  @ManyToOne
  @JoinColumn(name = "id_validator_employee")
  private EmployeeEntity validatorEmployee;

  @OneToMany(mappedBy = "inventory")
  private List<EmissionEntity> emissions;

  @OneToMany(mappedBy = "inventory")
  private List<ReductionEntity> reductions;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "status", nullable = false, columnDefinition = "INVENTORY_STATUS")
  private InventoryStatusEnum status;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "type", nullable = false, columnDefinition = "INVENTORY_TYPE")
  private InventoryTypeEnum type;

  @OneToOne
  @JoinColumn(name = "id_storage_file")
  private StorageFileEntity storageFile;

  public InventoryEntity(String name, InventoryTypeEnum type) {
    this.name = name;
    this.status = InventoryStatusEnum.UNDER_REVIEW;
    this.type = type;
  }

  public InventoryEntity(String name, InventoryTypeEnum type, EmployeeEntity ownerEmployee, StorageFileEntity storageFile) {
    this.name = name;
    this.status = InventoryStatusEnum.UNDER_REVIEW;
    this.type = type;
    this.ownerEmployee = ownerEmployee;
    this.department = ownerEmployee.getDepartment();
    this.storageFile = storageFile;
  }
}
