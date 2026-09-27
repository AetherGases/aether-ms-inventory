package com.aether.ms_inventory.inventory;

import com.aether.ms_inventory.shared.enums.EmployeeStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.*;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.*;
import com.aether.ms_inventory.shared.security.jwt.JwtTokenProvider;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("InventoryController Tests")
class InventoryControllerTests {

  private static final String REGISTERED_INVENTORY_NAME = "Inventário Registro Teste";
  private static final String REGISTERED_FILE_NAME = "arquivo-registro-teste.xlsx";
  private static final String REGISTERED_FILE_PATH = "https://cloudinary/arquivo-registro-teste.xlsx";
  private static final String ENTERPRISE_CNPJ = "73414740000148";
  private static final String UNIT_CNPJ = "89307523000199";
  private static final String EMPLOYEE_ACTIVE_EMAIL = "test@test1.com";
  private static final String EMPLOYEE_OTHER_EMAIL = "test@test2.com";
  private static final String PERMISSION_GROUP_NAME = "Funcionário";
  private static final String PERMISSION_NAME = "Relatórios";
  private static final String DEPARTMENT_NAME = "test depto";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private StorageFileRepository storageFileRepository;

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
  private JwtTokenProvider jwtTokenProvider;

  private PermissionGroupEntity permissionGroup;
  private EnterpriseEntity enterprise;
  private UnitEntity unit;
  private DepartmentEntity department;

  private EmployeeEntity employeeActive;
  private EmployeeEntity employeeOther;
  private PermissionEntity permission;

  // IDs criados durante o teste atual, sempre removidos no @AfterEach
  private final List<Integer> inventoriesIds = new ArrayList<>();
  private final List<Integer> storageFileIds = new ArrayList<>();

  @BeforeAll
  void beforeAll() {
    // Remove resíduos de execuções anteriores que falharam antes de concluir o afterAll,
    // evitando duplicidade de CNPJ e FKs pendentes (employee <- inventory)
    cleanupLeftoverTestData();

    enterprise = new EnterpriseEntity(
        "teste",
        "teste s.a.",
        ENTERPRISE_CNPJ
    );

    enterpriseRepository.save(enterprise);

    unit = new UnitEntity(
        "1234567",
        UNIT_CNPJ,
        enterprise
    );

    unitRepository.save(unit);

    department = new DepartmentEntity(
        DEPARTMENT_NAME,
        "departamento de teste",
        unit
    );

    departmentRepository.save(department);

    permission = new PermissionEntity(
        PERMISSION_NAME,
        null,
        "^.*/inventories.*$"
    );
    permissionRepository.save(permission);

    permissionGroup = new PermissionGroupEntity(PERMISSION_GROUP_NAME);
    permissionGroup.setPermissions(List.of(permission));
    permissionGroupRepository.save(permissionGroup);

    employeeActive = new EmployeeEntity(
        "12345678912",
        "Teste",
        EMPLOYEE_ACTIVE_EMAIL,
        "senhaHash",
        "11977394517",
        EmployeeStatusEnum.ACTIVE,
        permissionGroup,
        department
    );

    employeeActive = employeeRepository.save(employeeActive);

    // Segundo funcionário, usado para garantir que um usuário não enxerga os inventários de outro
    employeeOther = new EmployeeEntity(
        "52998224725",
        "Outro Teste",
        EMPLOYEE_OTHER_EMAIL,
        "senhaHash",
        "11988887777",
        EmployeeStatusEnum.ACTIVE,
        permissionGroup,
        department
    );

    employeeOther = employeeRepository.save(employeeOther);
  }

  /**
   * Remove qualquer dado de uma execução anterior que tenha ficado "preso" no banco
   * porque a suíte não terminou normalmente (ex.: falha no meio do beforeAll/afterEach).
   * Sem isso, o CNPJ fixo do enterprise e os relacionamentos de employee/inventory
   * colidem com os novos dados criados nesta execução.
   */
  private void cleanupLeftoverTestData() {

    List<EmployeeEntity> leftoverEmployees = employeeRepository.findAll().stream()
        .filter(e -> EMPLOYEE_ACTIVE_EMAIL.equals(e.getEmail()) || EMPLOYEE_OTHER_EMAIL.equals(e.getEmail()))
        .toList();

    List<Integer> leftoverEmployeeIds = leftoverEmployees.stream()
        .map(EmployeeEntity::getId)
        .toList();

    // Qualquer inventário pendurado nesses funcionários (criado por createInventory
    // ou pelo POST) precisa sair antes, senão a FK trava a remoção do employee
    if (!leftoverEmployeeIds.isEmpty()) {
      List<Integer> leftoverInventoryIds = inventoryRepository.findAll().stream()
          .filter(inv -> inv.getOwnerEmployee() != null
              && leftoverEmployeeIds.contains(inv.getOwnerEmployee().getId()))
          .map(InventoryEntity::getId)
          .toList();

      if (!leftoverInventoryIds.isEmpty()) {
        inventoryRepository.deleteAllByIdInBatch(leftoverInventoryIds);
      }
    }

    List<Integer> leftoverFileIds = storageFileRepository.findAll().stream()
        .filter(f -> REGISTERED_FILE_NAME.equalsIgnoreCase(f.getName()))
        .map(StorageFileEntity::getId)
        .toList();

    if (!leftoverFileIds.isEmpty()) {
      storageFileRepository.deleteAllByIdInBatch(leftoverFileIds);
    }

    if (!leftoverEmployeeIds.isEmpty()) {
      employeeRepository.deleteAllByIdInBatch(leftoverEmployeeIds);
    }

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

  @AfterAll()
  void afterAll() {

    if (employeeOther != null && employeeOther.getId() != null) {
      employeeRepository.deleteById(employeeOther.getId());
    }

    if (employeeActive != null && employeeActive.getId() != null) {
      employeeRepository.deleteById(employeeActive.getId());
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
    // Deleção em lote (bulk DELETE), sem carregar/gerenciar as entidades na sessão:
    // evita que um flush acabe validando estado pendente de outros testes.
    if (!inventoriesIds.isEmpty()) {
      inventoryRepository.deleteAllByIdInBatch(inventoriesIds);
      inventoriesIds.clear();
    }

    if (!storageFileIds.isEmpty()) {
      storageFileRepository.deleteAllByIdInBatch(storageFileIds);
      storageFileIds.clear();
    }
  }

  private String generateJwt(EmployeeEntity employee) {
    return jwtTokenProvider
        .createAccessToken(
            employee.getEmail(),
            List.of()
        )
        .accessToken();
  }

  private InventoryEntity createInventory(String name) {
    return createInventory(name, employeeActive);
  }

  private InventoryEntity createInventory(String name, EmployeeEntity owner) {
    InventoryEntity inventory = new InventoryEntity();

    inventory.setName(name);
    inventory.setDepartment(department);
    inventory.setOwnerEmployee(owner);
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);
    inventory.setType(InventoryTypeEnum.INPUT);

    inventoryRepository.save(inventory);

    inventoriesIds.add(inventory.getId());

    return inventory;
  }

  private String registerBody(String name, String fileName, String path) {
    return """
        {"name": "%s", "fileName": "%s", "path": "%s"}
        """.formatted(name, fileName, path);
  }

  // ---------------------------------------------------------------------------
  // GET /api/inventories/mine
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 200 and my inventories")
  void findMyInventoriesSuccess() throws Exception {

    createInventory("Inventário Teste");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should return 200 when no query parameters are provided")
  void findMyInventoriesWithoutQueryParameters() throws Exception {

    createInventory("Inventário Teste");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should filter inventories by name")
  void findMyInventoriesByName() throws Exception {

    createInventory("Inventário A");
    createInventory("Inventário B");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine")
                .param("name", "Inventário A")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should filter inventories by status")
  void findMyInventoriesByStatus() throws Exception {

    createInventory("Inventário A");
    createInventory("Inventário B");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine")
                .param("status", InventoryStatusEnum.UNDER_REVIEW.name())
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories.length()").value(2))
        .andExpect(jsonPath("$.totalCount").value(2));
  }

  @Test
  @DisplayName("Should not return inventories owned by other employees")
  void findMyInventoriesOnlyReturnsOwnInventories() throws Exception {

    createInventory("Inventário Meu");
    createInventory("Inventário De Outro", employeeOther);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.inventories[0].name").value("Inventário Meu"))
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should apply take parameter")
  void findMyInventoriesWithTake() throws Exception {

    createInventory("Inventário A");
    createInventory("Inventário B");
    createInventory("Inventário C");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine")
                .param("take", "2")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.inventories.length()").value(2))
        .andExpect(jsonPath("$.totalCount").value(3));
  }

  @Test
  @DisplayName("Should apply skip and take parameters")
  void findMyInventoriesWithSkipAndTake() throws Exception {

    createInventory("Inventário A");
    createInventory("Inventário B");
    createInventory("Inventário C");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine")
                .param("skip", "1")
                .param("take", "1")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.totalCount").value(3));
  }

  @Test
  @DisplayName("Should return 200 and empty list when user has no inventories")
  void findMyInventoriesEmpty() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isEmpty())
        .andExpect(jsonPath("$.totalCount").value(0));
  }

  @Test
  @DisplayName("Should return 401 when JWT is not provided")
  void findMyInventoriesWithoutAuthentication() throws Exception {

    mockMvc.perform(
            get("/api/inventories/mine")
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when JWT is invalid")
  void findMyInventoriesWithInvalidToken() throws Exception {

    mockMvc.perform(
            get("/api/inventories/mine")
                .header(
                    "Authorization",
                    "Bearer token-invalido"
                )
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 400 when take is less than minimum")
  void findMyInventoriesWithInvalidTakeMin() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine")
                .param("take", "0")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when take is greater than maximum")
  void findMyInventoriesWithInvalidTakeMax() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine")
                .param(
                    "take",
                    String.valueOf(
                        com.aether.ms_inventory.shared.AetherConstants.MAX_TAKE + 1
                    )
                )
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when status is not a valid enum value")
  void findMyInventoriesWithInvalidStatus() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine")
                .param("status", "STATUS_INEXISTENTE")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------------------------
  // POST /api/inventories
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 201 and register the inventory with its storage file")
  void registerInventorySuccess() throws Exception {

    String token = generateJwt(employeeActive);

    MvcResult result = mockMvc.perform(
            post("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(
                    REGISTERED_INVENTORY_NAME,
                    REGISTERED_FILE_NAME,
                    REGISTERED_FILE_PATH
                ))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value(REGISTERED_INVENTORY_NAME))
        .andExpect(jsonPath("$.type").value(InventoryTypeEnum.INPUT.name()))
        .andExpect(jsonPath("$.storageFile.id").isNumber())
        .andReturn();

    String responseBody = result.getResponse().getContentAsString();

    int inventoryId = JsonPath.read(responseBody, "$.id");
    int storageFileId = JsonPath.read(responseBody, "$.storageFile.id");

    // Garante que o cleanup remova exatamente o que este teste criou,
    // por ID em vez de por nome (imune a qualquer transformação de string a jusante)
    inventoriesIds.add(inventoryId);
    storageFileIds.add(storageFileId);

    InventoryEntity savedInventory = inventoryRepository.findById(inventoryId).orElse(null);
    StorageFileEntity savedFile = storageFileRepository.findById(storageFileId).orElse(null);

    assertNotNull(savedInventory, "Inventário deveria ter sido persistido no banco");
    assertNotNull(savedFile, "Arquivo deveria ter sido persistido no banco");
    assertEquals(REGISTERED_FILE_PATH, savedFile.getPath());
    assertNotNull(savedInventory.getStorageFile(), "Inventário deveria estar associado ao arquivo");
    assertEquals(savedFile.getId(), savedInventory.getStorageFile().getId());
  }

  @Test
  @DisplayName("Should return 401 when registering without JWT")
  void registerInventoryWithoutAuthentication() throws Exception {

    mockMvc.perform(
            post("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(
                    REGISTERED_INVENTORY_NAME,
                    REGISTERED_FILE_NAME,
                    REGISTERED_FILE_PATH
                ))
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when registering with an invalid JWT")
  void registerInventoryWithInvalidToken() throws Exception {

    mockMvc.perform(
            post("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(
                    REGISTERED_INVENTORY_NAME,
                    REGISTERED_FILE_NAME,
                    REGISTERED_FILE_PATH
                ))
                .header("Authorization", "Bearer token-invalido")
        )
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest(name = "Should return 400 for invalid body: {0}")
  @ValueSource(strings = {
      "{}",
      "{\"name\":\"\",\"fileName\":\"arquivo.csv\",\"path\":\"/uploads/arquivo.csv\"}",
      "{\"name\":\"Inventário\",\"fileName\":\"\",\"path\":\"/uploads/arquivo.csv\"}",
      "{\"name\":\"Inventário\",\"fileName\":\"arquivo.csv\",\"path\":\"\"}",
      "{invalid-json"
  })
  void registerInventoryWithInvalidBody(String body) throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            post("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when registering without a request body")
  void registerInventoryWithoutBody() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            post("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------------------------
  // GET /api/inventories/pending
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 200 and pending inventories")
  void findManyPendingInventoriesSuccess() throws Exception {

    createInventory("Inventário Pendente");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/pending")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should return 200 when no query parameters are provided")
  void findManyPendingInventoriesWithoutQueryParameters() throws Exception {

    createInventory("Inventário Pendente");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/pending")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should filter pending inventories by name")
  void findManyPendingInventoriesByName() throws Exception {

    createInventory("Inventário A");
    createInventory("Inventário B");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/pending")
                .param("name", "Inventário A")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should return pending inventories from every employee in the department")
  void findManyPendingInventoriesIncludesOtherEmployees() throws Exception {

    createInventory("Inventário Meu");
    createInventory("Inventário De Outro", employeeOther);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/pending")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories.length()").value(2))
        .andExpect(jsonPath("$.totalCount").value(2));
  }

  @Test
  @DisplayName("Should apply take parameter")
  void findManyPendingInventoriesWithTake() throws Exception {

    createInventory("Inventário A");
    createInventory("Inventário B");
    createInventory("Inventário C");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/pending")
                .param("take", "2")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.inventories.length()").value(2))
        .andExpect(jsonPath("$.totalCount").value(3));
  }

  @Test
  @DisplayName("Should apply skip and take parameters")
  void findManyPendingInventoriesWithSkipAndTake() throws Exception {

    createInventory("Inventário A");
    createInventory("Inventário B");
    createInventory("Inventário C");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/pending")
                .param("skip", "1")
                .param("take", "1")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.totalCount").value(3));
  }

  @Test
  @DisplayName("Should return 200 and empty list when there are no pending inventories")
  void findManyPendingInventoriesEmpty() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/pending")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isEmpty())
        .andExpect(jsonPath("$.totalCount").value(0));
  }

  @Test
  @DisplayName("Should return 401 when JWT is not provided")
  void findManyPendingInventoriesWithoutAuthentication() throws Exception {

    mockMvc.perform(
            get("/api/inventories/pending")
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when JWT is invalid")
  void findManyPendingInventoriesWithInvalidToken() throws Exception {

    mockMvc.perform(
            get("/api/inventories/pending")
                .header(
                    "Authorization",
                    "Bearer token-invalido"
                )
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 400 when take is less than minimum")
  void findManyPendingInventoriesWithInvalidTakeMin() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/pending")
                .param("take", "0")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when take is greater than maximum")
  void findManyPendingInventoriesWithInvalidTakeMax() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/pending")
                .param(
                    "take",
                    String.valueOf(
                        com.aether.ms_inventory.shared.AetherConstants.MAX_TAKE + 1
                    )
                )
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }
}