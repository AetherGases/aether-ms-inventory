package com.aether.ms_inventory.seal;

import com.aether.ms_inventory.shared.enums.CategoryClassificationEnum;
import com.aether.ms_inventory.shared.enums.EmployeeStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.*;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.*;
import com.aether.ms_inventory.shared.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("SealController Tests")
class SealControllerTests {

  private static final String ENTERPRISE_CNPJ = "11222333000181";
  private static final String UNIT_CNPJ = "11444777000161";
  private static final String EMPLOYEE_EMAIL = "test@seal1.com";
  private static final String EMPLOYEE_CPF = "11144477735";
  private static final String PERMISSION_GROUP_NAME = "Funcionário Selo Teste";
  private static final String PERMISSION_NAME = "Selo Teste";
  private static final String DEPARTMENT_NAME = "test depto selo";
  private static final String REDUCTION_CATEGORY_NAME = "Categoria Teste Redução Selo";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private ReductionRepository reductionRepository;

  @Autowired
  private EnterpriseRepository enterpriseRepository;

  @Autowired
  private UnitRepository unitRepository;

  @Autowired
  private DepartmentRepository departmentRepository;

  @Autowired
  private EmployeeRepository employeeRepository;

  @Autowired
  private PermissionRepository permissionRepository;

  @Autowired
  private PermissionGroupRepository permissionGroupRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private EnterpriseEntity enterprise;
  private UnitEntity unit;
  private DepartmentEntity department;
  private EmployeeEntity employee;
  private PermissionEntity permission;
  private PermissionGroupEntity permissionGroup;
  private CategoryEntity reductionCategory;

  private final List<Integer> inventoriesIds = new ArrayList<>();
  private final List<Integer> reductionIds = new ArrayList<>();

  @BeforeAll
  void beforeAll() {
    cleanupLeftoverTestData();

    enterprise = new EnterpriseEntity("teste selo", "teste selo s.a.", ENTERPRISE_CNPJ);
    enterpriseRepository.save(enterprise);

    unit = new UnitEntity("7654321", UNIT_CNPJ, enterprise);
    unitRepository.save(unit);

    department = new DepartmentEntity(DEPARTMENT_NAME, "departamento de teste do selo", unit);
    departmentRepository.save(department);

    permission = new PermissionEntity(PERMISSION_NAME, null, "^.*/seal.*$");
    permissionRepository.save(permission);

    permissionGroup = new PermissionGroupEntity(PERMISSION_GROUP_NAME);
    permissionGroup.setPermissions(List.of(permission));
    permissionGroupRepository.save(permissionGroup);

    employee = new EmployeeEntity(
        EMPLOYEE_CPF,
        "Teste Selo",
        EMPLOYEE_EMAIL,
        "senhaHash",
        "11977394518",
        EmployeeStatusEnum.ACTIVE,
        permissionGroup,
        department
    );
    employee = employeeRepository.save(employee);

    reductionCategory = new CategoryEntity();
    reductionCategory.setName(REDUCTION_CATEGORY_NAME);
    reductionCategory.setClassification(CategoryClassificationEnum.DOWNSTREAM);
    reductionCategory = categoryRepository.save(reductionCategory);
  }

  private void cleanupLeftoverTestData() {
    List<Integer> leftoverEmployeeIds = employeeRepository.findAll().stream()
        .filter(e -> EMPLOYEE_EMAIL.equals(e.getEmail()))
        .map(EmployeeEntity::getId)
        .toList();

    if (!leftoverEmployeeIds.isEmpty()) {
      List<Integer> leftoverInventoryIds = inventoryRepository.findAll().stream()
          .filter(inv -> inv.getOwnerEmployee() != null
              && leftoverEmployeeIds.contains(inv.getOwnerEmployee().getId()))
          .map(InventoryEntity::getId)
          .toList();

      if (!leftoverInventoryIds.isEmpty()) {
        reductionRepository.findAll().stream()
            .filter(r -> r.getInventory() != null && leftoverInventoryIds.contains(r.getInventory().getId()))
            .map(ReductionEntity::getId)
            .forEach(reductionRepository::deleteById);
        inventoryRepository.deleteAllByIdInBatch(leftoverInventoryIds);
      }

      employeeRepository.deleteAllByIdInBatch(leftoverEmployeeIds);
    }

    categoryRepository.findAll().stream()
        .filter(c -> REDUCTION_CATEGORY_NAME.equals(c.getName()))
        .map(CategoryEntity::getId)
        .forEach(categoryRepository::deleteById);

    permissionGroupRepository.findAll().stream()
        .filter(pg -> PERMISSION_GROUP_NAME.equals(pg.getDescription()))
        .map(PermissionGroupEntity::getId)
        .forEach(permissionGroupRepository::deleteById);

    permissionRepository.findAll().stream()
        .filter(p -> PERMISSION_NAME.equals(p.getName()))
        .map(PermissionEntity::getId)
        .forEach(permissionRepository::deleteById);

    departmentRepository.findAll().stream()
        .filter(d -> DEPARTMENT_NAME.equals(d.getName()))
        .map(DepartmentEntity::getId)
        .forEach(departmentRepository::deleteById);

    unitRepository.findAll().stream()
        .filter(u -> UNIT_CNPJ.equals(u.getCnpj()))
        .map(UnitEntity::getId)
        .forEach(unitRepository::deleteById);

    enterpriseRepository.findAll().stream()
        .filter(e -> ENTERPRISE_CNPJ.equals(e.getCnpj()))
        .map(EnterpriseEntity::getId)
        .forEach(enterpriseRepository::deleteById);
  }

  @AfterAll
  void afterAll() {
    if (reductionCategory != null && reductionCategory.getId() != null) {
      categoryRepository.deleteById(reductionCategory.getId());
    }

    if (employee != null && employee.getId() != null) {
      employeeRepository.deleteById(employee.getId());
    }

    if (permissionGroup != null && permissionGroup.getId() != null) {
      permissionGroupRepository.deleteById(permissionGroup.getId());
    }

    if (permission != null && permission.getId() != null) {
      permissionRepository.deleteById(permission.getId());
    }

    if (department != null && department.getId() != null) {
      departmentRepository.deleteById(department.getId());
    }

    if (unit != null && unit.getId() != null) {
      unitRepository.delete(unit);
    }

    if (enterprise != null && enterprise.getId() != null) {
      enterpriseRepository.delete(enterprise);
    }
  }

  @AfterEach
  void cleanup() {
    if (!reductionIds.isEmpty()) {
      reductionRepository.deleteAllByIdInBatch(reductionIds);
      reductionIds.clear();
    }

    if (!inventoriesIds.isEmpty()) {
      inventoryRepository.deleteAllByIdInBatch(inventoriesIds);
      inventoriesIds.clear();
    }
  }

  private String generateJwt() {
    return jwtTokenProvider
        .createAccessToken(employee.getEmail(), List.of())
        .accessToken();
  }

  private InventoryEntity createInventory(InventoryStatusEnum status) {
    InventoryEntity inventory = new InventoryEntity();

    inventory.setName("Inventário Selo");
    inventory.setDepartment(department);
    inventory.setOwnerEmployee(employee);
    inventory.setStatus(status);
    inventory.setType(InventoryTypeEnum.INPUT);

    inventoryRepository.save(inventory);
    inventoriesIds.add(inventory.getId());

    return inventory;
  }

  private void createReduction(InventoryEntity inventory) {
    ReductionEntity reduction = new ReductionEntity();
    reduction.setQuantityCo2e(new BigDecimal("10.5"));
    reduction.setCategory(reductionCategory);
    reduction.setInventory(inventory);
    reductionRepository.save(reduction);

    reductionIds.add(reduction.getId());
  }

  // ---------------------------------------------------------------------------
  // GET /api/seal/progress
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 200 with zero points when there is no approved inventory")
  void findSealProgressWithoutApprovedInventory() throws Exception {
    createInventory(InventoryStatusEnum.UNDER_REVIEW);

    mockMvc.perform(
            get("/api/seal/progress")
                .header("Authorization", "Bearer " + generateJwt())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPoints").value(0.0))
        .andExpect(jsonPath("$.minimumPoints").value(1.6))
        .andExpect(jsonPath("$.category").doesNotExist())
        .andExpect(jsonPath("$.nextCategory").value("D"))
        .andExpect(jsonPath("$.pointsToNextCategory").value(1.6))
        .andExpect(jsonPath("$.progressPercent").value(0.0))
        .andExpect(jsonPath("$.criteriaMet").value(0))
        .andExpect(jsonPath("$.criteriaTotal").value(4))
        .andExpect(jsonPath("$.criteria.length()").value(4));
  }

  @Test
  @DisplayName("Should return 200 with 0.70 points when there is an approved inventory")
  void findSealProgressWithApprovedInventory() throws Exception {
    createInventory(InventoryStatusEnum.APPROVED);

    mockMvc.perform(
            get("/api/seal/progress")
                .header("Authorization", "Bearer " + generateJwt())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPoints").value(0.7))
        .andExpect(jsonPath("$.nextCategory").value("D"))
        .andExpect(jsonPath("$.pointsToNextCategory").value(0.9))
        .andExpect(jsonPath("$.progressPercent").value(43.75))
        .andExpect(jsonPath("$.criteriaMet").value(1))
        .andExpect(jsonPath("$.criteria[0].code").value("GHG_DECLARATION"))
        .andExpect(jsonPath("$.criteria[0].points").value(0.7))
        .andExpect(jsonPath("$.criteria[0].maxPoints").value(1.0))
        .andExpect(jsonPath("$.criteria[0].tracked").value(true));
  }

  @Test
  @DisplayName("Should return 200 with 1.00 point when approved inventory has reductions")
  void findSealProgressWithApprovedInventoryAndReductions() throws Exception {
    InventoryEntity inventory = createInventory(InventoryStatusEnum.APPROVED);
    createReduction(inventory);

    mockMvc.perform(
            get("/api/seal/progress")
                .header("Authorization", "Bearer " + generateJwt())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPoints").value(1.0))
        .andExpect(jsonPath("$.pointsToNextCategory").value(0.6))
        .andExpect(jsonPath("$.progressPercent").value(62.5))
        .andExpect(jsonPath("$.criteria[0].points").value(1.0));
  }

  @Test
  @DisplayName("Should return untracked criteria with zero points")
  void findSealProgressUntrackedCriteria() throws Exception {
    mockMvc.perform(
            get("/api/seal/progress")
                .header("Authorization", "Bearer " + generateJwt())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.criteria[1].code").value("CERTIFICATES"))
        .andExpect(jsonPath("$.criteria[1].tracked").value(false))
        .andExpect(jsonPath("$.criteria[1].points").value(0.0))
        .andExpect(jsonPath("$.criteria[2].code").value("GOALS"))
        .andExpect(jsonPath("$.criteria[2].tracked").value(false))
        .andExpect(jsonPath("$.criteria[3].code").value("OTHER_ACTIONS"))
        .andExpect(jsonPath("$.criteria[3].tracked").value(false));
  }

  @Test
  @DisplayName("Should return 401 when JWT is not provided")
  void findSealProgressWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/seal/progress"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when JWT is invalid")
  void findSealProgressWithInvalidToken() throws Exception {
    mockMvc.perform(
            get("/api/seal/progress")
                .header("Authorization", "Bearer token-invalido")
        )
        .andExpect(status().isUnauthorized());
  }
}
