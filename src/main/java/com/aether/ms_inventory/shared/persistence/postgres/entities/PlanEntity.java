package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "plan")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PlanEntity extends DateBaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(
      nullable = false,
      unique = true,
      length = AetherConstants.MAX_PLAN_NAME_LENGTH
  )
  private String name;

  @Column(length = AetherConstants.MAX_PLAN_DESCRIPTION_LENGTH)
  private String description;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "duration_days", nullable = false)
  private Integer durationDays;

  @Column(name = "is_active", nullable = false)
  @ColumnDefault("true")
  private Boolean isActive;

  @OneToMany(mappedBy = "plan")
  private List<PlanSubscriptionEntity> planSubscriptions;
}