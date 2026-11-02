package com.aether.ms_inventory.seal.services;

import com.aether.ms_inventory.seal.dto.input.FindSealProgressInputDTO;
import com.aether.ms_inventory.seal.dto.output.FindSealProgressOutputDTO;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.SealCategoryEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.DepartmentEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.ReductionRepository;
import com.aether.ms_inventory.shared.services.GetUserInfosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calcula o progresso da empresa no Selo Clima Paraná, seguindo o Termo de Referência
 * 2025 (Organizações Privadas). Os pontos vêm de quatro critérios:
 * Declaração de GEE, Certificados, Metas e Outras ações.
 * <p>
 * Apenas a Declaração de GEE pode ser derivada dos dados atuais do sistema.
 * Os demais critérios retornam zero e {@code tracked = false} até existir uma fonte de dados.
 */
@Service
@RequiredArgsConstructor
public class SealService {
  private static final BigDecimal GHG_VERIFIED_POINTS = new BigDecimal("0.70");
  private static final BigDecimal GHG_VERIFIED_WITH_REDUCTION_POINTS = new BigDecimal("1.00");
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final InventoryRepository inventoryRepository;
  private final ReductionRepository reductionRepository;
  private final GetUserInfosService getUserInfosService;

  @Transactional(readOnly = true)
  public FindSealProgressOutputDTO findSealProgress(FindSealProgressInputDTO input) {
    DepartmentEntity department = getUserInfosService.getDepartment(input.userId());
    Integer enterpriseId = department.getUnit().getEnterprise().getId();

    List<FindSealProgressOutputDTO.Criterion> criteria = List.of(
        calculateGhgDeclaration(enterpriseId),
        untrackedCriterion("CERTIFICATES", "Certificados de terceira parte"),
        untrackedCriterion("GOALS", "Metas"),
        untrackedCriterion("OTHER_ACTIONS", "Outras ações")
    );

    BigDecimal totalPoints = criteria.stream()
        .map(FindSealProgressOutputDTO.Criterion::points)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);

    return buildProgress(totalPoints, criteria);
  }
  private FindSealProgressOutputDTO.Criterion calculateGhgDeclaration(Integer enterpriseId) {
    boolean hasApprovedInventory = inventoryRepository
        .countByStatusAndDepartmentUnitEnterpriseId(InventoryStatusEnum.APPROVED, enterpriseId) > 0;

    BigDecimal reductions = reductionRepository.sumQuantityCo2eByEnterpriseId(enterpriseId);
    boolean hasReduction = reductions != null && reductions.signum() > 0;

    BigDecimal points = BigDecimal.ZERO;

    if (hasApprovedInventory) {
      points = hasReduction ? GHG_VERIFIED_WITH_REDUCTION_POINTS : GHG_VERIFIED_POINTS;
    }

    return new FindSealProgressOutputDTO.Criterion(
        "GHG_DECLARATION",
        "Declaração de GEE",
        points.setScale(2, RoundingMode.HALF_UP),
        GHG_VERIFIED_WITH_REDUCTION_POINTS.setScale(2, RoundingMode.HALF_UP),
        true
    );
  }

  private FindSealProgressOutputDTO.Criterion untrackedCriterion(String code, String name) {
    return new FindSealProgressOutputDTO.Criterion(
        code,
        name,
        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
        null,
        false
    );
  }

  private FindSealProgressOutputDTO buildProgress(
      BigDecimal totalPoints,
      List<FindSealProgressOutputDTO.Criterion> criteria
  ) {
    int criteriaMet = (int) criteria.stream()
        .filter(criterion -> criterion.points().signum() > 0)
        .count();
    int criteriaTotal = criteria.size();

    SealCategoryEnum[] categories = SealCategoryEnum.values();
    SealCategoryEnum current = null;
    SealCategoryEnum next = categories[0];

    for (int i = 0; i < categories.length; i++) {
      if (totalPoints.compareTo(categories[i].getMinimumPoints()) >= 0) {
        current = categories[i];
        next = i + 1 < categories.length ? categories[i + 1] : null;
      }
    }

    BigDecimal minimumPoints = categories[0].getMinimumPoints();

    if (next == null) {
      return new FindSealProgressOutputDTO(
          totalPoints,
          minimumPoints,
          current.name(),
          null,
          null,
          HUNDRED.setScale(2, RoundingMode.HALF_UP),
          criteriaMet,
          criteriaTotal,
          criteria
      );
    }

    BigDecimal floor = current == null ? BigDecimal.ZERO : current.getMinimumPoints();
    BigDecimal range = next.getMinimumPoints().subtract(floor);
    BigDecimal progressPercent = totalPoints.subtract(floor)
        .multiply(HUNDRED)
        .divide(range, 2, RoundingMode.HALF_UP);

    return new FindSealProgressOutputDTO(
        totalPoints,
        minimumPoints,
        current == null ? null : current.name(),
        next.name(),
        next.getMinimumPoints().subtract(totalPoints).setScale(2, RoundingMode.HALF_UP),
        progressPercent,
        criteriaMet,
        criteriaTotal,
        criteria
    );
  }
}
