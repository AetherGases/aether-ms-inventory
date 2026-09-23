package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "storage_file")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class StorageFileEntity extends DateBaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = AetherConstants.MAX_STORAGE_FILE_NAME_LENGTH, nullable = false)
  private String name;

  @Column(length = AetherConstants.MAX_STORAGE_FILE_PATH_LENGTH, nullable = false)
  private String path;

  @OneToOne(mappedBy = "storageFile")
  private EmployeeEntity employee;

  @OneToOne(mappedBy = "storageFile")
  private InventoryEntity inventory;

  public StorageFileEntity(String name, String path) {
    this.name = name;
    this.path = path;
  }
}
