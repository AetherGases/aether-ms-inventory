package com.aether.ms_inventory.shared.persistence.postgres.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "administrator")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AdministratorEntity extends UserBaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
}
