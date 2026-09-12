package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "emission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmissionEntity extends DateBaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(
      name = "quantity_co2e",
      nullable = false
  )
  private BigDecimal quantityCo2e;

  @Column(
      name = "methodology_description",
      length = AetherConstants.MAX_EMISSION_METHODOLOGY_DESCRIPTION_LENGTH
  )
  private String methodologyDescription;

  @Column(
      name = "supplier_data_percentage"
  )
  private BigDecimal supplierDataPercentage;

  @ManyToOne
  @JoinColumn(name = "id_gas")
  private GasEntity gas;

  @ManyToOne
  @JoinColumn(name = "id_scope")
  private ScopeEntity scope;

  @ManyToOne
  @JoinColumn(name = "id_category")
  private CategoryEntity category;

  @ManyToOne
  @JoinColumn(name = "id_inventory")
  private InventoryEntity inventory;
}