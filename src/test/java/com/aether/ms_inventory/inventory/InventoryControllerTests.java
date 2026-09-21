package com.aether.ms_inventory.inventory;

import com.aether.ms_inventory.shared.enums.EmployeeStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.*;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.*;
import com.aether.ms_inventory.shared.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  private static final String REGISTERED_FILE_NAME = "arquivo-registro-teste.csv";
  private static final String REGISTERED_FILE_PATH = "/uploads/arquivo-registro-teste.csv";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private StorageFileRepository storageFileRepository;

  @Autowired
  private EmployeeRepository employeeRepository;

  @Autowired
  private PermissionRepository permissionRepository;

  @Autowired
  private PermissionGroupRepository permissionGroupRepository;

  @Autowired
  private DepartmentRepository departmentRepository;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private EmployeeEntity employeeActive;
  private EmployeeEntity employeeOther;
  private PermissionEntity permission;
  private PermissionGroupEntity permissionGroup;
  private DepartmentEntity department;
  private List<Integer> inventoriesIds = new ArrayList<>();

  @BeforeAll
  void beforeAll() {
    department = new DepartmentEntity();
    department.setName("Departamento Teste");
    department = departmentRepository.save(department);

    permission = new PermissionEntity(
        "Relatórios",
        null,
        "^.*/inventories.*$"
    );
    permissionRepository.save(permission);

    permissionGroup = new PermissionGroupEntity("Funcionário");
    permissionGroup.setPermissions(List.of(permission));
    permissionGroupRepository.save(permissionGroup);

    employeeActive = new EmployeeEntity(
        "12345678909",
        "Teste",
        "test@test1.com",
        "senhaHash",
        "11977394517",
        EmployeeStatusEnum.ACTIVE,
        List.of(permissionGroup)
    );

    employeeActive.setDepartment(department);
    employeeActive = employeeRepository.save(employeeActive);

    // Segundo funcionário, usado para garantir que um usuário não enxerga os inventários de outro
    employeeOther = new EmployeeEntity(
        "52998224725",
        "Outro Teste",
        "test@test2.com",
        "senhaHash",
        "11988887777",
        EmployeeStatusEnum.ACTIVE,
        List.of(permissionGroup)
    );

    employeeOther.setDepartment(department);
    employeeOther = employeeRepository.save(employeeOther);
  }

  @AfterAll()
  void afterAll(){
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
  }

  @AfterEach
  void cleanup() {
    // Inventários criados via POST não passam pelo createInventory, então são localizados pelo nome
    inventoryRepository.findAll().stream()
        .filter(inventory -> REGISTERED_INVENTORY_NAME.equals(inventory.getName()))
        .map(InventoryEntity::getId)
        .forEach(inventoriesIds::add);

    if (!inventoriesIds.isEmpty()) {
      inventoryRepository.deleteAllByIdInBatch(inventoriesIds);

      inventoriesIds.clear();
    }

    // O arquivo só pode ser removido depois do inventário (FK)
    List<StorageFileEntity> registeredFiles = storageFileRepository.findAll().stream()
        .filter(file -> REGISTERED_FILE_NAME.equals(file.getName()))
        .toList();

    if (!registeredFiles.isEmpty()) {
      storageFileRepository.deleteAll(registeredFiles);
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

  private InventoryEntity createInventory(
      String name
  ) {
    return createInventory(name, employeeActive);
  }

  private InventoryEntity createInventory(
      String name,
      EmployeeEntity owner
  ) {
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

    mockMvc.perform(
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
        .andExpect(jsonPath("$.type").value(InventoryTypeEnum.INPUT.name()));

    long inventoriesCount = inventoryRepository.findAll().stream()
        .filter(inventory -> REGISTERED_INVENTORY_NAME.equals(inventory.getName()))
        .count();

    long filesCount = storageFileRepository.findAll().stream()
        .filter(file -> REGISTERED_FILE_NAME.equals(file.getName()))
        .count();

    assertEquals(1L, inventoriesCount);
    assertEquals(1L, filesCount);
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
}