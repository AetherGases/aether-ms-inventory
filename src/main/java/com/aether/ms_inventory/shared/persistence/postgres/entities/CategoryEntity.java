package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import com.aether.ms_inventory.shared.enums.CategoryClassificationEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "category")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CategoryEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = AetherConstants.MAX_CATEGORY_NAME_LENGTH, nullable = false, unique = true)
  private String name;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "CATEGORY_CLASSIFICATION")
  private CategoryClassificationEnum classification;

  @OneToMany(mappedBy = "category")
  private List<EmissionEntity> emissions;

  @OneToMany(mappedBy = "category")
  private List<ReductionEntity> reductions;
}

