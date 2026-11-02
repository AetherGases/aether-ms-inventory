package com.aether.ms_inventory.seal;

import com.aether.ms_inventory.seal.dto.input.FindSealProgressInputDTO;
import com.aether.ms_inventory.seal.dto.output.FindSealProgressOutputDTO;
import com.aether.ms_inventory.seal.services.SealService;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.DepartmentEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EnterpriseEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.UnitEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.ReductionRepository;
import com.aether.ms_inventory.shared.services.GetUserInfosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("SealService Tests")
public class SealServiceTests {

  private static final Integer USER_ID = 1;
  private static final Integer ENTERPRISE_ID = 5;

  @Mock
  private InventoryRepository inventoryRepository;

  @Mock
  private ReductionRepository reductionRepository;

  @Mock
  private GetUserInfosService getUserInfosService;

  @InjectMocks
  private SealService sealService;

  @BeforeEach
  void setUp() {
    EnterpriseEntity enterprise = new EnterpriseEntity();
    enterprise.setId(ENTERPRISE_ID);

    UnitEntity unit = new UnitEntity();
    unit.setEnterprise(enterprise);

    DepartmentEntity department = new DepartmentEntity();
    department.setUnit(unit);

    when(getUserInfosService.getDepartment(USER_ID)).thenReturn(department);
  }

  private void givenApprovedInventories(long count) {
    when(inventoryRepository.countByStatusAndDepartmentUnitEnterpriseId(
        InventoryStatusEnum.APPROVED, ENTERPRISE_ID
    )).thenReturn(count);
  }

  private void givenReductions(BigDecimal total) {
    when(reductionRepository.sumQuantityCo2eByEnterpriseId(ENTERPRISE_ID)).thenReturn(total);
  }

  private FindSealProgressOutputDTO.Criterion ghgOf(FindSealProgressOutputDTO output) {
    return output.criteria().stream()
        .filter(c -> "GHG_DECLARATION".equals(c.code()))
        .findFirst()
        .orElseThrow();
  }

  @Test
  @DisplayName("Should give zero points when enterprise has no approved inventory")
  void findSealProgressWithoutApprovedInventory() {
    givenApprovedInventories(0);
    givenReductions(null);

    FindSealProgressOutputDTO output = sealService.findSealProgress(new FindSealProgressInputDTO(USER_ID));

    assertEquals(new BigDecimal("0.00"), output.totalPoints());
    assertEquals(new BigDecimal("1.60"), output.minimumPoints());
    assertNull(output.category());
    assertEquals("D", output.nextCategory());
    assertEquals(new BigDecimal("1.60"), output.pointsToNextCategory());
    assertEquals(new BigDecimal("0.00"), output.progressPercent());
    assertEquals(0, output.criteriaMet());
    assertEquals(4, output.criteriaTotal());
    assertEquals(new BigDecimal("0.00"), ghgOf(output).points());
  }

  @Test
  @DisplayName("Should give 0.70 points when there is an approved inventory without reductions")
  void findSealProgressWithApprovedInventoryOnly() {
    givenApprovedInventories(1);
    givenReductions(null);

    FindSealProgressOutputDTO output = sealService.findSealProgress(new FindSealProgressInputDTO(USER_ID));

    assertEquals(new BigDecimal("0.70"), output.totalPoints());
    assertNull(output.category());
    assertEquals("D", output.nextCategory());
    assertEquals(new BigDecimal("0.90"), output.pointsToNextCategory());
    assertEquals(new BigDecimal("43.75"), output.progressPercent());
    assertEquals(1, output.criteriaMet());
    assertEquals(new BigDecimal("0.70"), ghgOf(output).points());
  }

  @Test
  @DisplayName("Should treat zero reductions as no reduction")
  void findSealProgressWithZeroReductions() {
    givenApprovedInventories(2);
    givenReductions(BigDecimal.ZERO);

    FindSealProgressOutputDTO output = sealService.findSealProgress(new FindSealProgressInputDTO(USER_ID));

    assertEquals(new BigDecimal("0.70"), output.totalPoints());
  }

  @Test
  @DisplayName("Should give 1.00 point when there is an approved inventory with reductions")
  void findSealProgressWithApprovedInventoryAndReductions() {
    givenApprovedInventories(1);
    givenReductions(new BigDecimal("10.5"));

    FindSealProgressOutputDTO output = sealService.findSealProgress(new FindSealProgressInputDTO(USER_ID));

    assertEquals(new BigDecimal("1.00"), output.totalPoints());
    assertNull(output.category());
    assertEquals("D", output.nextCategory());
    assertEquals(new BigDecimal("0.60"), output.pointsToNextCategory());
    assertEquals(new BigDecimal("62.50"), output.progressPercent());
    assertEquals(1, output.criteriaMet());
    assertEquals(new BigDecimal("1.00"), ghgOf(output).points());
  }

  @Test
  @DisplayName("Should give zero points when there are reductions but no approved inventory")
  void findSealProgressWithReductionsButNoApprovedInventory() {
    givenApprovedInventories(0);
    givenReductions(new BigDecimal("10.5"));

    FindSealProgressOutputDTO output = sealService.findSealProgress(new FindSealProgressInputDTO(USER_ID));

    assertEquals(new BigDecimal("0.00"), output.totalPoints());
    assertEquals(0, output.criteriaMet());
  }

  @Test
  @DisplayName("Should return the four criteria, with only GHG declaration tracked")
  void findSealProgressCriteriaStructure() {
    givenApprovedInventories(1);
    givenReductions(null);

    FindSealProgressOutputDTO output = sealService.findSealProgress(new FindSealProgressInputDTO(USER_ID));

    assertEquals(4, output.criteria().size());

    FindSealProgressOutputDTO.Criterion ghg = ghgOf(output);
    assertEquals("Declaração de GEE", ghg.name());
    assertEquals(new BigDecimal("1.00"), ghg.maxPoints());
    assertTrue(ghg.tracked());

    output.criteria().stream()
        .filter(c -> !"GHG_DECLARATION".equals(c.code()))
        .forEach(c -> {
          assertFalse(c.tracked());
          assertNull(c.maxPoints());
          assertEquals(new BigDecimal("0.00"), c.points());
        });

    assertEquals(
        java.util.List.of("GHG_DECLARATION", "CERTIFICATES", "GOALS", "OTHER_ACTIONS"),
        output.criteria().stream().map(FindSealProgressOutputDTO.Criterion::code).toList()
    );
  }

  @Test
  @DisplayName("Should query data using the enterprise of the user's department")
  void findSealProgressUsesUserEnterprise() {
    givenApprovedInventories(1);
    givenReductions(null);

    sealService.findSealProgress(new FindSealProgressInputDTO(USER_ID));

    verify(getUserInfosService).getDepartment(USER_ID);
    verify(inventoryRepository).countByStatusAndDepartmentUnitEnterpriseId(
        InventoryStatusEnum.APPROVED, ENTERPRISE_ID
    );
    verify(reductionRepository).sumQuantityCo2eByEnterpriseId(ENTERPRISE_ID);
  }
}
