package com.aether.ms_inventory.inventory;

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

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("InventoryController Tests")
class InventoryControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private InventoryRepository inventoryRepository;

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

  private final List<Integer> ids = new ArrayList<>();

  private EmployeeEntity employeeActive;
  private PermissionEntity permission;
  private PermissionGroupEntity permissionGroup;
  private DepartmentEntity department;
  private List<Integer> inventoriesIds;

  @BeforeEach
  void setup() {

    department = new DepartmentEntity();

    department.setName("Departamento Teste");

    department = departmentRepository.save(department);

    permission = new PermissionEntity(
        "Relatórios",
        null,
        "^.*/inventories.*$"
    );

    permissionRepository.save(permission);

    permissionGroup = new PermissionGroupEntity(
        "Funcionário"
    );

    permissionGroup.setPermissions(List.of(permission));

    permissionGroupRepository.save(permissionGroup);

    employeeActive = new EmployeeEntity(
        "12345678902",
        "Teste",
        "test@test.com",
        "senhaHash",
        "11977394517",
        EmployeeStatusEnum.ACTIVE,
        List.of(permissionGroup)
    );

    employeeActive.setDepartment(department);

    employeeActive = employeeRepository.save(employeeActive);

    ids.add(employeeActive.getId());
    ids.add(department.getId());
  }

  @AfterEach
  void cleanup() {
    inventoryRepository.deleteAllByIdInBatch(
        inventoriesIds
    );

    employeeRepository.deleteAllByIdInBatch(
        List.of(employeeActive.getId())
    );

    permissionGroupRepository.deleteAllByIdInBatch(
        List.of(permissionGroup.getId())
    );

    permissionRepository.deleteAllByIdInBatch(
        List.of(permission.getId())
    );

    departmentRepository.deleteAllByIdInBatch(
        List.of(department.getId())
    );

    ids.clear();
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
    InventoryEntity inventory = new InventoryEntity();

    inventoriesIds.add(inventory.getId());
    inventory.setName(name);
    inventory.setDepartment(department);
    inventory.setOwnerEmployee(employeeActive);
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);
    inventory.setType(InventoryTypeEnum.INPUT);

    return inventoryRepository.save(inventory);
  }

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
}