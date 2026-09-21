package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "permission")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PermissionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = AetherConstants.MAX_PERMISSION_NAME_LENGTH, nullable = false, unique = true)
  private String name;

  @Column(length = AetherConstants.MAX_PERMISSION_DESCRIPTION_LENGTH)
  private String description;

  @Column(length = AetherConstants.MAX_PERMISSION_URL_LENGTH)
  private String url;

  @ManyToMany(mappedBy = "permissions")
  private List<PermissionGroupEntity> permissionGroups;

  public PermissionEntity(String name, String description, String url) {
    this.name = name;
    this.description = description;
    this.url = url;
  }
}
