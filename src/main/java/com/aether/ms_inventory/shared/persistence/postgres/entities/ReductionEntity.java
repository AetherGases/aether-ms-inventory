package com.aether.ms_inventory.shared.persistence.postgres.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "reduction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReductionEntity extends DateBaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(
      name = "quantity_co2e",
      nullable = false
  )
  private BigDecimal quantityCo2e;

  @ManyToOne
  @JoinColumn(name = "id_inventory")
  private InventoryEntity inventory;

  @ManyToOne
  @JoinColumn(name = "id_category")
  private CategoryEntity category;
}