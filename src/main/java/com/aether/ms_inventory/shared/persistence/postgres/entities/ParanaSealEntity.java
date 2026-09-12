package com.aether.ms_inventory.shared.persistence.postgres.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "parana_seal_forecast")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ParanaSealEntity extends DateBaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private BigDecimal score;
  private Integer level;

  @Column(name = "valid_until")
  private LocalDate validUntil;

  @ManyToOne
  @JoinColumn(name = "id_unit")
  private UnitEntity unit;
}
