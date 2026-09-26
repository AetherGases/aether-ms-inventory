package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "enterprise")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EnterpriseEntity extends DateBaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = AetherConstants.MAX_ENTERPRISE_NAME_LENGTH, nullable = false)
  private String name;

  @Column(length = AetherConstants.MAX_ENTERPRISE_TRADE_NAME_LENGTH)
  private String tradeName;

  @Column(length = AetherConstants.CNPJ_LENGTH, unique = true, nullable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private String cnpj;

  @ManyToOne
  @JoinColumn(name = "id_address")
  private AddressEntity address;

  @OneToMany(mappedBy = "enterprise")
  private List<PlanSubscriptionEntity> planSubscriptions;

  @OneToMany(mappedBy = "enterprise")
  private List<UnitEntity> units;

  public EnterpriseEntity(String name, String tradeName, String cnpj) {
    this.name = name;
    this.tradeName = tradeName;
    this.cnpj = cnpj;
  }
}
