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
@Table(name = "address")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AddressEntity extends DateBaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(
      name = "zip_code",
      length = AetherConstants.ZIP_CODE_LENGTH
  )
  @JdbcTypeCode(SqlTypes.CHAR)
  private String zipCode;

  @Column(length = AetherConstants.MAX_STATE_LENGTH)
  private String state;

  @Column(length = AetherConstants.MAX_CITY_LENGTH, nullable = false)
  private String city;

  @Column(length = AetherConstants.MAX_NEIGHBORHOOD_LENGTH, nullable = false)
  private String neighborhood;

  @Column(length = AetherConstants.MAX_ADDRESS_COMPLEMENT_LENGTH, nullable = false)
  private String street;

  @Column(nullable = false)
  private Integer number;

  @Column(length = AetherConstants.MAX_ADDRESS_COMPLEMENT_LENGTH)
  private String complement;

  @OneToMany(mappedBy = "address")
  private List<EnterpriseEntity> enterprises;

  @OneToMany(mappedBy = "address")
  private List<UnitEntity> units;
}
