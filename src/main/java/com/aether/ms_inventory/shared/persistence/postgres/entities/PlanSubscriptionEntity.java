package com.aether.ms_inventory.shared.persistence.postgres.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "plan_subscription")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PlanSubscriptionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "is_active")
  @ColumnDefault("true")
  private Boolean active;

  @Column(nullable = false)
  private Integer installments;

  @Column(
      name = "created_at",
      nullable = false,
      insertable = false,
      updatable = false,
      columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
  )
  private LocalDateTime createdAt;

  @Column(name = "deactivated_at")
  private LocalDateTime deactivatedAt;

  @ManyToOne
  @JoinColumn(name = "id_plan")
  private PlanEntity plan;

  @ManyToOne
  @JoinColumn(name = "id_enterprise")
  private EnterpriseEntity enterprise;

  @OneToMany(mappedBy = "planSubscription")
  private List<PaymentEntity> payments;
}
