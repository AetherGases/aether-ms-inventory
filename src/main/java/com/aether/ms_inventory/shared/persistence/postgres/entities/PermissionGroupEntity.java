package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "permission_group")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PermissionGroupEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = AetherConstants.MAX_PERMISSION_GROUP_DESCRIPTION_LENGTH)
  private String description;

  @ManyToMany
  @JoinTable(
      name = "permission_group_permission",
      joinColumns = @JoinColumn(name = "id_permission_group"),
      inverseJoinColumns = @JoinColumn(name = "id_permission")
  )
  private List<PermissionEntity> permissions;

  @ManyToMany(mappedBy = "permissionGroups")
  private List<EmployeeEntity> employees;

  public PermissionGroupEntity(String description) {
    this.description = description;
  }
}
