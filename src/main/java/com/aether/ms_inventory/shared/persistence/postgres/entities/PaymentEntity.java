package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.enums.PaymentStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentEntity extends DateBaseEntity{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  private BigDecimal value;

  private LocalDateTime term;

  @Column(name = "additional_use_value")
  private BigDecimal additionalUseValue;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "PAYMENT_STATUS")
  private PaymentStatusEnum status;

  @ManyToOne
  @JoinColumn(name = "id_plan_subscription")
  private PlanSubscriptionEntity planSubscription;
}
