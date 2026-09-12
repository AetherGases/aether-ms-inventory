package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "scope")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ScopeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = AetherConstants.MAX_SCOPE_NAME_LENGTH, nullable = false, unique = true)
  private String name;

  @OneToMany(mappedBy = "scope")
  private List<EmissionEntity> emissions;
}
