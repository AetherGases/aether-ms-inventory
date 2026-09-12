package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "department")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DepartmentEntity extends DateBaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = AetherConstants.MAX_DEPARTMENT_NAME_LENGTH, nullable = false)
  private String name;

  @Column(length = AetherConstants.MAX_DEPARTMENT_DESCRIPTION_LENGTH)
  private String description;
  @ManyToOne
  @JoinColumn(name = "id_unit")
  private UnitEntity unit;

  @OneToMany(mappedBy = "department")
  private List<EmployeeEntity> employee;

  @OneToMany(mappedBy = "department")
  private List<InventoryEntity> inventories;
}
