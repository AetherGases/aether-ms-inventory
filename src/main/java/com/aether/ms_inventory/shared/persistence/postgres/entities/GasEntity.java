package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "gas")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GasEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = AetherConstants.MAX_GAS_NAME_LENGTH, nullable = false)
  private String name;

  @Column(length = AetherConstants.MAX_GAS_FORMULA_LENGTH, nullable = false, unique = true)
  private String formula;

  @Column(name = "is_biogenic", nullable = false)
  private Boolean biogenic;

  @Column(nullable = false)
  private BigDecimal gwp;

  @OneToMany(mappedBy = "gas")
  private List<EmissionEntity> emissions;
}
