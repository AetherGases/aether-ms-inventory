package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "unit")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UnitEntity extends DateBaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = AetherConstants.CNAE_LENGTH)
  @JdbcTypeCode(SqlTypes.CHAR)
  private String cnae;

  @Column(length = AetherConstants.CNPJ_LENGTH, nullable = false, unique = true)
  @JdbcTypeCode(SqlTypes.CHAR)
  private String cnpj;

  @Column(name = "is_active")
  @ColumnDefault("true")
  private Boolean isActive;

  @ManyToOne
  @JoinColumn(name = "id_enterprise")
  private EnterpriseEntity enterprise;

  @ManyToOne
  @JoinColumn(name = "id_address")
  private AddressEntity address;

  @OneToMany(mappedBy = "unit")
  private List<DepartmentEntity> departments;

  @OneToMany(mappedBy = "unit")
  private List<ParanaSealEntity> paranaSeals;

  public UnitEntity(String cnae, String cnpj, EnterpriseEntity enterprise) {
    this.cnae = cnae;
    this.cnpj = cnpj;
    this.isActive = true;
    this.enterprise = enterprise;
  }
}
