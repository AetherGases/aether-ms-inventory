package com.aether.ms_inventory.inventory;

import com.aether.ms_inventory.shared.enums.EmployeeStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.*;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.*;
import com.aether.ms_inventory.shared.persistence.redis.repositories.InputInventoryRepository;
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
import com.aether.ms_inventory.shared.persistence.redis.entities.InputInventoryDocument;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
  private static final String REGISTERED_CONSOLIDATION_APPROACH = "Participação acionária";
  private static final String ENTERPRISE_CNPJ = "73414740000148";
  private static final String UNIT_CNPJ = "89307523000199";
  private static final String EMPLOYEE_ACTIVE_EMAIL = "test@test1.com";
  private static final String EMPLOYEE_OTHER_EMAIL = "test@test2.com";
  private static final String PERMISSION_GROUP_NAME = "Funcionário";
  private static final String PERMISSION_NAME = "Relatórios";
  private static final String DEPARTMENT_NAME = "test depto";
  private static final String OTHER_DEPARTMENT_NAME = "test depto outro";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private InputInventoryRepository inputInventoryRepository;
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
  private DepartmentEntity otherDepartment;

  private EmployeeEntity employeeActive;
  private EmployeeEntity employeeOther;
  private PermissionEntity permission;
  private final List<Integer> inventoriesIds = new ArrayList<>();
  private final List<Integer> storageFileIds = new ArrayList<>();

  @BeforeAll
  void beforeAll() {
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
  private void cleanupLeftoverTestData() {

    List<EmployeeEntity> leftoverEmployees = employeeRepository.findAll().stream()
        .filter(e -> EMPLOYEE_ACTIVE_EMAIL.equals(e.getEmail()) || EMPLOYEE_OTHER_EMAIL.equals(e.getEmail()))
        .toList();

    List<Integer> leftoverEmployeeIds = leftoverEmployees.stream()
        .map(EmployeeEntity::getId)
        .toList();

    if (!leftoverEmployeeIds.isEmpty()) {
      List<Integer> leftoverInventoryIds = inventoryRepository.findAll().stream()
          .filter(inv -> inv.getOwnerEmployee() != null
              && leftoverEmployeeIds.contains(inv.getOwnerEmployee().getId()))
          .map(InventoryEntity::getId)
          .toList();

      if (!leftoverInventoryIds.isEmpty()) {
        inventoryRepository.deleteAllByIdInBatch(leftoverInventoryIds);
        leftoverInventoryIds.forEach(inputInventoryRepository::deleteById);
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
        .filter(d -> OTHER_DEPARTMENT_NAME.equals(d.getName()) || DEPARTMENT_NAME.equals(d.getName()))
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

    if (otherDepartment != null && otherDepartment.getId() != null) {
      departmentRepository.deleteById(otherDepartment.getId());
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
    if (!inventoriesIds.isEmpty()) {
      inventoriesIds.forEach(inputInventoryRepository::deleteById);
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
    return registerBody(name, REGISTERED_CONSOLIDATION_APPROACH, fileName, path);
  }

  private String registerBody(String name, String consolidationApproach, String fileName, String path) {
    return """
        {"name": "%s", "consolidationApproach": "%s", "fileName": "%s", "path": "%s"}
        """.formatted(name, consolidationApproach, fileName, path);
  }

  private InventoryEntity createInventory(
      String name,
      EmployeeEntity owner,
      DepartmentEntity dept,
      InventoryStatusEnum status
  ) {
    InventoryEntity inventory = new InventoryEntity();

    inventory.setName(name);
    inventory.setDepartment(dept);
    inventory.setOwnerEmployee(owner);
    inventory.setStatus(status);
    inventory.setType(InventoryTypeEnum.INPUT);

    inventoryRepository.save(inventory);

    inventoriesIds.add(inventory.getId());

    return inventory;
  }

  private InventoryEntity createInventory(
      String name,
      EmployeeEntity owner,
      InventoryStatusEnum status,
      InventoryTypeEnum type
  ) {
    InventoryEntity inventory = new InventoryEntity();

    inventory.setName(name);
    inventory.setDepartment(department);
    inventory.setOwnerEmployee(owner);
    inventory.setStatus(status);
    inventory.setType(type);

    inventoryRepository.save(inventory);

    inventoriesIds.add(inventory.getId());

    return inventory;
  }

  private InventoryEntity createReviewedInventory(
      String name,
      EmployeeEntity owner,
      EmployeeEntity validator,
      InventoryStatusEnum status
  ) {
    InventoryEntity inventory = new InventoryEntity();

    inventory.setName(name);
    inventory.setDepartment(department);
    inventory.setOwnerEmployee(owner);
    inventory.setValidatorEmployee(validator);
    inventory.setStatus(status);
    inventory.setType(InventoryTypeEnum.INPUT);

    inventoryRepository.save(inventory);

    inventoriesIds.add(inventory.getId());

    return inventory;
  }

  private String rejectBody(String message) {
    return """
        {"message": "%s"}
        """.formatted(message);
  }

  private String approveBody(String message) {
    return """
        {"message": "%s"}
        """.formatted(message);
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

    inventoriesIds.add(inventoryId);
    storageFileIds.add(storageFileId);

    InventoryEntity savedInventory = inventoryRepository.findById(inventoryId).orElse(null);
    StorageFileEntity savedFile = storageFileRepository.findById(storageFileId).orElse(null);
    InputInventoryDocument savedDocument = inputInventoryRepository.findById(inventoryId);

    assertNotNull(savedInventory, "Inventário deveria ter sido persistido no banco");
    assertEquals(
        REGISTERED_CONSOLIDATION_APPROACH.trim().toLowerCase(),
        savedInventory.getConsolidationApproach()
    );
    assertNotNull(savedFile, "Arquivo deveria ter sido persistido no banco");
    assertEquals(REGISTERED_FILE_PATH, savedFile.getPath());
    assertNotNull(savedInventory.getStorageFile(), "Inventário deveria estar associado ao arquivo");
    assertEquals(savedFile.getId(), savedInventory.getStorageFile().getId());
    assertNotNull(savedDocument, "Documento do inventário deveria ter sido persistido no Redis");
    assertEquals(inventoryId, savedDocument.getId());
    assertEquals(savedInventory.getStatus(), savedDocument.getStatus());
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
      "{\"name\":\"\",\"consolidationApproach\":\"Participação acionária\",\"fileName\":\"arquivo.csv\",\"path\":\"/uploads/arquivo.csv\"}",
      "{\"name\":\"Inventário\",\"consolidationApproach\":\"\",\"fileName\":\"arquivo.csv\",\"path\":\"/uploads/arquivo.csv\"}",
      "{\"name\":\"Inventário\",\"consolidationApproach\":\"Participação acionária\",\"fileName\":\"\",\"path\":\"/uploads/arquivo.csv\"}",
      "{\"name\":\"Inventário\",\"consolidationApproach\":\"Participação acionária\",\"fileName\":\"arquivo.csv\",\"path\":\"\"}",
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

    createInventory("Inventário Pendente", employeeOther);

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

    createInventory("Inventário Pendente", employeeOther);

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

    createInventory("Inventário A", employeeOther);
    createInventory("Inventário B", employeeOther);

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
  @DisplayName("Should return pending inventories from other employees but not the requester's own")
  void findManyPendingInventoriesIncludesOtherEmployees() throws Exception {

    createInventory("Inventário Meu");
    createInventory("Inventário De Outro", employeeOther);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/pending")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.inventories[0].name").value("Inventário de Outro"))
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should apply take parameter")
  void findManyPendingInventoriesWithTake() throws Exception {

    createInventory("Inventário A", employeeOther);
    createInventory("Inventário B", employeeOther);
    createInventory("Inventário C", employeeOther);

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

    createInventory("Inventário A", employeeOther);
    createInventory("Inventário B", employeeOther);
    createInventory("Inventário C", employeeOther);

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

  // ---------------------------------------------------------------------------
  // PATCH /api/inventories/pending/{id}/reject
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 200 and reject an inventory under review")
  void rejectInventorySuccess() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente", employeeOther);
    String message = "Dados inconsistentes no arquivo";

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/reject", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectBody(message))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk());

    InventoryEntity updated = inventoryRepository.findById(inventory.getId()).orElseThrow();

    assertEquals(InventoryStatusEnum.REJECTED, updated.getStatus());
    assertEquals(message, updated.getStatusMessage());
  }

  @Test
  @DisplayName("Should no longer list a rejected inventory as pending")
  void rejectInventoryRemovesItFromPendingList() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente", employeeOther);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/reject", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectBody("Motivo"))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk());

    mockMvc.perform(
            get("/api/inventories/pending")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isEmpty())
        .andExpect(jsonPath("$.totalCount").value(0));
  }

  @Test
  @DisplayName("Should return 404 when the inventory does not exist")
  void rejectInventoryNotFound() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/reject", Integer.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectBody("Motivo"))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when the inventory belongs to another department")
  void rejectInventoryFromAnotherDepartment() throws Exception {

    InventoryEntity inventory = createInventory(
        "Inventário Outro Depto",
        employeeOther,
        otherDepartment,
        InventoryStatusEnum.UNDER_REVIEW
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/reject", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectBody("Motivo"))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());

    InventoryEntity unchanged = inventoryRepository.findById(inventory.getId()).orElseThrow();
    assertEquals(InventoryStatusEnum.UNDER_REVIEW, unchanged.getStatus());
  }

  @Test
  @DisplayName("Should return 404 when the inventory is not under review")
  void rejectInventoryNotUnderReview() throws Exception {

    InventoryEntity inventory = createInventory(
        "Inventário Já Rejeitado",
        employeeActive,
        department,
        InventoryStatusEnum.REJECTED
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/reject", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectBody("Novo motivo"))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());

    InventoryEntity unchanged = inventoryRepository.findById(inventory.getId()).orElseThrow();
    assertEquals(InventoryStatusEnum.REJECTED, unchanged.getStatus());
    assertNotEquals("Novo motivo", unchanged.getStatusMessage());
  }

  @Test
  @DisplayName("Should return 401 when rejecting without JWT")
  void rejectInventoryWithoutAuthentication() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente");

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/reject", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectBody("Motivo"))
        )
        .andExpect(status().isUnauthorized());

    InventoryEntity unchanged = inventoryRepository.findById(inventory.getId()).orElseThrow();
    assertEquals(InventoryStatusEnum.UNDER_REVIEW, unchanged.getStatus());
  }

  @Test
  @DisplayName("Should return 401 when rejecting with an invalid JWT")
  void rejectInventoryWithInvalidToken() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente");

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/reject", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectBody("Motivo"))
                .header("Authorization", "Bearer token-invalido")
        )
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest(name = "Should return 400 for invalid reject body: {0}")
  @ValueSource(strings = {
      "{}",
      "{\"message\":\"\"}",
      "{\"message\":\"   \"}",
      "{invalid-json"
  })
  void rejectInventoryWithInvalidBody(String body) throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/reject", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());

    InventoryEntity unchanged = inventoryRepository.findById(inventory.getId()).orElseThrow();
    assertEquals(InventoryStatusEnum.UNDER_REVIEW, unchanged.getStatus());
  }

  @Test
  @DisplayName("Should return 400 when rejecting without a request body")
  void rejectInventoryWithoutBody() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/reject", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when the id is not a number")
  void rejectInventoryWithInvalidId() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/reject", "abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectBody("Motivo"))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------------------------
  // PATCH /api/inventories/pending/{id}/approve
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 200 and approve an inventory under review")
  void approveInventorySuccess() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente", employeeOther);
    String message = "Dados conferidos";

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/approve", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveBody(message))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(inventory.getId()))
        .andExpect(jsonPath("$.status").value(InventoryStatusEnum.APPROVED.name()))
        .andExpect(jsonPath("$.validationMessage").value(message));

    InventoryEntity updated = inventoryRepository.findById(inventory.getId()).orElseThrow();

    assertEquals(InventoryStatusEnum.APPROVED, updated.getStatus());
    assertEquals(message, updated.getStatusMessage());
  }

  @Test
  @DisplayName("Should no longer list an approved inventory as pending")
  void approveInventoryRemovesItFromPendingList() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente", employeeOther);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/approve", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveBody("Motivo"))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk());

    mockMvc.perform(
            get("/api/inventories/pending")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isEmpty())
        .andExpect(jsonPath("$.totalCount").value(0));
  }

  @Test
  @DisplayName("Should return 404 when approving an inventory that does not exist")
  void approveInventoryNotFound() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/approve", Integer.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveBody("Motivo"))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when approving an inventory from another department")
  void approveInventoryFromAnotherDepartment() throws Exception {

    InventoryEntity inventory = createInventory(
        "Inventário Outro Depto",
        employeeOther,
        otherDepartment,
        InventoryStatusEnum.UNDER_REVIEW
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/approve", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveBody("Motivo"))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());

    InventoryEntity unchanged = inventoryRepository.findById(inventory.getId()).orElseThrow();
    assertEquals(InventoryStatusEnum.UNDER_REVIEW, unchanged.getStatus());
  }

  @Test
  @DisplayName("Should return 404 when approving an inventory that is already rejected")
  void approveInventoryAlreadyRejected() throws Exception {

    InventoryEntity inventory = createInventory(
        "Inventário Já Rejeitado",
        employeeActive,
        department,
        InventoryStatusEnum.REJECTED
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/approve", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveBody("Novo motivo"))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());

    InventoryEntity unchanged = inventoryRepository.findById(inventory.getId()).orElseThrow();
    assertEquals(InventoryStatusEnum.REJECTED, unchanged.getStatus());
    assertNotEquals("Novo motivo", unchanged.getStatusMessage());
  }

  @Test
  @DisplayName("Should return 404 when approving an inventory that is already approved")
  void approveInventoryAlreadyApproved() throws Exception {

    InventoryEntity inventory = createInventory(
        "Inventário Já Aprovado",
        employeeActive,
        department,
        InventoryStatusEnum.APPROVED
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/approve", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveBody("Novo motivo"))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());

    InventoryEntity unchanged = inventoryRepository.findById(inventory.getId()).orElseThrow();
    assertEquals(InventoryStatusEnum.APPROVED, unchanged.getStatus());
    assertNotEquals("Novo motivo", unchanged.getStatusMessage());
  }

  @Test
  @DisplayName("Should return 401 when approving without JWT")
  void approveInventoryWithoutAuthentication() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente");

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/approve", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveBody("Motivo"))
        )
        .andExpect(status().isUnauthorized());

    InventoryEntity unchanged = inventoryRepository.findById(inventory.getId()).orElseThrow();
    assertEquals(InventoryStatusEnum.UNDER_REVIEW, unchanged.getStatus());
  }

  @Test
  @DisplayName("Should return 401 when approving with an invalid JWT")
  void approveInventoryWithInvalidToken() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente");

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/approve", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveBody("Motivo"))
                .header("Authorization", "Bearer token-invalido")
        )
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest(name = "Should return 400 for invalid approve body: {0}")
  @ValueSource(strings = {
      "{}",
      "{\"message\":\"\"}",
      "{\"message\":\"   \"}",
      "{invalid-json"
  })
  void approveInventoryWithInvalidBody(String body) throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/approve", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());

    InventoryEntity unchanged = inventoryRepository.findById(inventory.getId()).orElseThrow();
    assertEquals(InventoryStatusEnum.UNDER_REVIEW, unchanged.getStatus());
  }

  @Test
  @DisplayName("Should return 400 when approving without a request body")
  void approveInventoryWithoutBody() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/approve", inventory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when the id is not a number")
  void approveInventoryWithInvalidId() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            patch("/api/inventories/pending/{id}/approve", "abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveBody("Motivo"))
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------------------------
  // GET /api/inventories/reviewed
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 200 and reviewed inventories")
  void findManyReviewedInventoriesSuccess() throws Exception {

    createInventory(
        "Inventário Aprovado",
        employeeOther,
        department,
        InventoryStatusEnum.APPROVED
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.inventories[0].name").value("Inventário Aprovado"))
        .andExpect(jsonPath("$.inventories[0].status").value(InventoryStatusEnum.APPROVED.name()))
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should return 200 when no query parameters are provided")
  void findManyReviewedInventoriesWithoutQueryParameters() throws Exception {

    createInventory(
        "Inventário Reprovado",
        employeeOther,
        department,
        InventoryStatusEnum.REJECTED
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should filter reviewed inventories by name")
  void findManyReviewedInventoriesByName() throws Exception {

    createInventory("Inventário A", employeeOther, department, InventoryStatusEnum.APPROVED);
    createInventory("Inventário B", employeeOther, department, InventoryStatusEnum.APPROVED);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
                .param("name", "Inventário A")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isArray())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should filter reviewed inventories by status")
  void findManyReviewedInventoriesByStatus() throws Exception {

    createInventory("Inventário Aprovado", employeeOther, department, InventoryStatusEnum.APPROVED);
    createInventory("Inventário Reprovado", employeeOther, department, InventoryStatusEnum.REJECTED);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
                .param("status", InventoryStatusEnum.APPROVED.name())
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.inventories[0].name").value("Inventário Aprovado"))
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should include reviewed inventories owned by the requesting user")
  void findManyReviewedInventoriesIncludesOwnInventories() throws Exception {

    createInventory("Inventário Meu", employeeActive, department, InventoryStatusEnum.APPROVED);
    createInventory("Inventário De Outro", employeeOther, department, InventoryStatusEnum.APPROVED);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories.length()").value(2))
        .andExpect(jsonPath("$.totalCount").value(2));
  }

  @Test
  @DisplayName("Should not return inventories that are still processing")
  void findManyReviewedInventoriesExcludesProcessing() throws Exception {

    createInventory("Inventário Processando", employeeOther, department, InventoryStatusEnum.PROCESSING);
    createInventory("Inventário Falhou", employeeOther, department, InventoryStatusEnum.PROCESSING_FAILED);
    createInventory("Inventário Requisitado", employeeOther, department, InventoryStatusEnum.REQUESTED);
    createInventory("Inventário Aprovado", employeeOther, department, InventoryStatusEnum.APPROVED);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories.length()").value(1))
        .andExpect(jsonPath("$.inventories[0].name").value("Inventário Aprovado"))
        .andExpect(jsonPath("$.totalCount").value(1));
  }

  @Test
  @DisplayName("Should apply take parameter")
  void findManyReviewedInventoriesWithTake() throws Exception {

    createInventory("Inventário A", employeeOther, department, InventoryStatusEnum.APPROVED);
    createInventory("Inventário B", employeeOther, department, InventoryStatusEnum.APPROVED);
    createInventory("Inventário C", employeeOther, department, InventoryStatusEnum.APPROVED);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
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
  void findManyReviewedInventoriesWithSkipAndTake() throws Exception {

    createInventory("Inventário A", employeeOther, department, InventoryStatusEnum.APPROVED);
    createInventory("Inventário B", employeeOther, department, InventoryStatusEnum.APPROVED);
    createInventory("Inventário C", employeeOther, department, InventoryStatusEnum.APPROVED);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
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
  @DisplayName("Should return 200 and empty list when there are no reviewed inventories")
  void findManyReviewedInventoriesEmpty() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventories").isEmpty())
        .andExpect(jsonPath("$.totalCount").value(0));
  }

  @Test
  @DisplayName("Should return 401 when JWT is not provided")
  void findManyReviewedInventoriesWithoutAuthentication() throws Exception {

    mockMvc.perform(
            get("/api/inventories/reviewed")
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when JWT is invalid")
  void findManyReviewedInventoriesWithInvalidToken() throws Exception {

    mockMvc.perform(
            get("/api/inventories/reviewed")
                .header(
                    "Authorization",
                    "Bearer token-invalido"
                )
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 400 when take is less than minimum")
  void findManyReviewedInventoriesWithInvalidTakeMin() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
                .param("take", "0")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when take is greater than maximum")
  void findManyReviewedInventoriesWithInvalidTakeMax() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
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
  void findManyReviewedInventoriesWithInvalidStatus() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed")
                .param("status", "STATUS_INEXISTENTE")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------------------------
  // GET /api/inventories/mine/{id}
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 200 and the inventory details owned by the user")
  void findMineInventoryByIdSuccess() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Meu");

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine/{id}", inventory.getId())
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(inventory.getId()))
        .andExpect(jsonPath("$.name").value("Inventário Meu"))
        .andExpect(jsonPath("$.status").value(InventoryStatusEnum.UNDER_REVIEW.name()));
  }

  @Test
  @DisplayName("Should return 404 when the inventory does not exist")
  void findMineInventoryByIdNotFound() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine/{id}", Integer.MAX_VALUE)
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when the inventory belongs to another employee")
  void findMineInventoryByIdFromAnotherEmployee() throws Exception {

    InventoryEntity inventory = createInventory("Inventário De Outro", employeeOther);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine/{id}", inventory.getId())
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 401 when JWT is not provided")
  void findMineInventoryByIdWithoutAuthentication() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Meu");

    mockMvc.perform(
            get("/api/inventories/mine/{id}", inventory.getId())
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when JWT is invalid")
  void findMineInventoryByIdWithInvalidToken() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Meu");

    mockMvc.perform(
            get("/api/inventories/mine/{id}", inventory.getId())
                .header("Authorization", "Bearer token-invalido")
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 400 when the id is not a number")
  void findMineInventoryByIdWithInvalidId() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine/{id}", "abc")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when the id is less than the minimum value")
  void findMineInventoryByIdWithIdBelowMinimum() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine/{id}", 0)
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------------------------
  // GET /api/inventories/reviewed/{id}
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 200 and the inventory details from the department")
  void findReviewedInventoryByIdSuccess() throws Exception {

    InventoryEntity inventory = createInventory(
        "Inventário Aprovado",
        employeeOther,
        department,
        InventoryStatusEnum.APPROVED
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed/{id}", inventory.getId())
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(inventory.getId()))
        .andExpect(jsonPath("$.name").value("Inventário Aprovado"))
        .andExpect(jsonPath("$.status").value(InventoryStatusEnum.APPROVED.name()));
  }

  @Test
  @DisplayName("Should return 200 for an inventory owned by the requesting user")
  void findReviewedInventoryByIdOwnedByRequester() throws Exception {

    InventoryEntity inventory = createInventory(
        "Inventário Meu Aprovado",
        employeeActive,
        department,
        InventoryStatusEnum.APPROVED
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed/{id}", inventory.getId())
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(inventory.getId()));
  }

  @Test
  @DisplayName("Should return 404 when the inventory does not exist")
  void findReviewedInventoryByIdNotFound() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed/{id}", Integer.MAX_VALUE)
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when the inventory belongs to another department")
  void findReviewedInventoryByIdFromAnotherDepartment() throws Exception {

    InventoryEntity inventory = createInventory(
        "Inventário Outro Depto",
        employeeOther,
        otherDepartment,
        InventoryStatusEnum.APPROVED
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed/{id}", inventory.getId())
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 401 when JWT is not provided")
  void findReviewedInventoryByIdWithoutAuthentication() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente");

    mockMvc.perform(
            get("/api/inventories/reviewed/{id}", inventory.getId())
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when JWT is invalid")
  void findReviewedInventoryByIdWithInvalidToken() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Pendente");

    mockMvc.perform(
            get("/api/inventories/reviewed/{id}", inventory.getId())
                .header("Authorization", "Bearer token-invalido")
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 400 when the id is not a number")
  void findReviewedInventoryByIdWithInvalidId() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed/{id}", "abc")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------------------------
  // GET /api/inventories/mine/last
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 200 and the owner's last input inventory")
  void findMyLastInventorySuccess() throws Exception {

    InventoryEntity inventory = createInventory("Inventário Meu", employeeActive);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine/last")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(inventory.getId()))
        .andExpect(jsonPath("$.name").value("Inventário Meu"))
        .andExpect(jsonPath("$.author.name").value("Teste"))
        .andExpect(jsonPath("$.status").value(InventoryStatusEnum.UNDER_REVIEW.name()))
        .andExpect(jsonPath("$.type").value(InventoryTypeEnum.INPUT.name()));
  }

  @Test
  @DisplayName("Should return the most recently created input inventory")
  void findMyLastInventoryMostRecent() throws Exception {

    createInventory("Inventário Antigo", employeeActive);
    Thread.sleep(10);
    InventoryEntity recent = createInventory("Inventário Recente", employeeActive);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine/last")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(recent.getId()))
        .andExpect(jsonPath("$.name").value("Inventário Recente"));
  }

  @Test
  @DisplayName("Should not return inventories owned by another employee")
  void findMyLastInventoryIgnoresOtherEmployees() throws Exception {

    createInventory("Inventário De Outro", employeeOther);

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine/last")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should not return inventories that are not of type INPUT")
  void findMyLastInventoryIgnoresNonInputType() throws Exception {

    createInventory(
        "Inventário Saída",
        employeeActive,
        InventoryStatusEnum.APPROVED,
        InventoryTypeEnum.OUTPUT
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine/last")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when the owner has no inventories")
  void findMyLastInventoryNotFound() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/mine/last")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 401 when JWT is not provided")
  void findMyLastInventoryWithoutAuthentication() throws Exception {

    mockMvc.perform(
            get("/api/inventories/mine/last")
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when JWT is invalid")
  void findMyLastInventoryWithInvalidToken() throws Exception {

    mockMvc.perform(
            get("/api/inventories/mine/last")
                .header("Authorization", "Bearer token-invalido")
        )
        .andExpect(status().isUnauthorized());
  }

  // ---------------------------------------------------------------------------
  // GET /api/inventories/reviewed/last
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 200 and the validator's last reviewed inventory")
  void findReviewedLastInventorySuccess() throws Exception {

    InventoryEntity inventory = createReviewedInventory(
        "Inventário Aprovado",
        employeeOther,
        employeeActive,
        InventoryStatusEnum.APPROVED
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed/last")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(inventory.getId()))
        .andExpect(jsonPath("$.name").value("Inventário Aprovado"))
        .andExpect(jsonPath("$.author.name").value("Outro Teste"))
        .andExpect(jsonPath("$.status").value(InventoryStatusEnum.APPROVED.name()));
  }

  @Test
  @DisplayName("Should return the most recently reviewed inventory")
  void findReviewedLastInventoryMostRecent() throws Exception {

    createReviewedInventory(
        "Inventário Antigo",
        employeeOther,
        employeeActive,
        InventoryStatusEnum.APPROVED
    );
    Thread.sleep(10);
    InventoryEntity recent = createReviewedInventory(
        "Inventário Recente",
        employeeOther,
        employeeActive,
        InventoryStatusEnum.REJECTED
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed/last")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(recent.getId()))
        .andExpect(jsonPath("$.name").value("Inventário Recente"));
  }

  @Test
  @DisplayName("Should not return inventories reviewed by another employee")
  void findReviewedLastInventoryIgnoresOtherValidators() throws Exception {

    createReviewedInventory(
        "Inventário De Outro Validador",
        employeeActive,
        employeeOther,
        InventoryStatusEnum.APPROVED
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed/last")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should not return inventories that are still processing")
  void findReviewedLastInventoryExcludesProcessing() throws Exception {

    createReviewedInventory(
        "Inventário Processando",
        employeeOther,
        employeeActive,
        InventoryStatusEnum.PROCESSING
    );

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed/last")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when the validator has no reviewed inventories")
  void findReviewedLastInventoryNotFound() throws Exception {

    String token = generateJwt(employeeActive);

    mockMvc.perform(
            get("/api/inventories/reviewed/last")
                .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 401 when JWT is not provided")
  void findReviewedLastInventoryWithoutAuthentication() throws Exception {

    mockMvc.perform(
            get("/api/inventories/reviewed/last")
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when JWT is invalid")
  void findReviewedLastInventoryWithInvalidToken() throws Exception {

    mockMvc.perform(
            get("/api/inventories/reviewed/last")
                .header("Authorization", "Bearer token-invalido")
        )
        .andExpect(status().isUnauthorized());
  }
}