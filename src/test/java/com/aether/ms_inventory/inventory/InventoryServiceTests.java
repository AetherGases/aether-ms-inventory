package com.aether.ms_inventory.inventory;

import com.aether.ms_inventory.inventory.dto.input.FindMyInventoriesHistoryInputDTO;
import com.aether.ms_inventory.inventory.dto.input.RegisterInventoryInputDTO;
import com.aether.ms_inventory.inventory.dto.output.FindMyInventoriesHistoryOutputDTO;
import com.aether.ms_inventory.inventory.dto.output.RegisterInventoryOutputDTO;
import com.aether.ms_inventory.inventory.services.InventoryService;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.DepartmentEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.StorageFileRepository;
import com.aether.ms_inventory.shared.services.GetUserInfosService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Tests")
public class InventoryServiceTests {

  @Mock
  private InventoryRepository inventoryRepository;

  @Mock
  private StorageFileRepository storageFileRepository;

  @Mock
  private GetUserInfosService getUserInfosService;

  @InjectMocks
  private InventoryService inventoryService;

  // ---------------------------------------------------------------------------
  // findMyInventoriesHistory
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return my inventories with pagination")
  void findMyInventoriesHistory() {
    Integer userId = 1;
    Integer departmentId = 10;

    FindMyInventoriesHistoryInputDTO input =
        new FindMyInventoriesHistoryInputDTO(
            userId,
            null,
            null,
            0,
            10
        );

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    InventoryEntity inventory = new InventoryEntity();

    List<InventoryEntity> inventories = List.of(inventory);

    when(getUserInfosService.getDepartment(userId))
        .thenReturn(department);

    when(inventoryRepository.findWithPagination(
        any(Specification.class),
        eq(input.skip()),
        eq(input.take())
    )).thenReturn(inventories);

    when(inventoryRepository.count(any(Specification.class)))
        .thenReturn(1L);

    FindMyInventoriesHistoryOutputDTO output =
        inventoryService.findMyInventoriesHistory(input);

    assertEquals(1, output.inventories().size());
    assertEquals(1L, output.totalCount());

    verify(getUserInfosService).getDepartment(userId);

    verify(inventoryRepository).findWithPagination(
        any(Specification.class),
        eq(input.skip()),
        eq(input.take())
    );

    verify(inventoryRepository).count(any(Specification.class));
  }

  @Test
  @DisplayName("Should return an empty list when there are no inventories")
  void findMyInventoriesHistoryEmpty() {
    Integer userId = 1;
    Integer departmentId = 10;

    FindMyInventoriesHistoryInputDTO input =
        new FindMyInventoriesHistoryInputDTO(
            userId,
            null,
            null,
            0,
            10
        );

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    when(getUserInfosService.getDepartment(userId))
        .thenReturn(department);

    when(inventoryRepository.findWithPagination(
        any(Specification.class),
        eq(input.skip()),
        eq(input.take())
    )).thenReturn(List.of());

    when(inventoryRepository.count(any(Specification.class)))
        .thenReturn(0L);

    FindMyInventoriesHistoryOutputDTO output =
        inventoryService.findMyInventoriesHistory(input);

    assertEquals(0, output.inventories().size());
    assertEquals(0L, output.totalCount());

    verify(inventoryRepository).findWithPagination(
        any(Specification.class),
        eq(input.skip()),
        eq(input.take())
    );

    verify(inventoryRepository).count(any(Specification.class));
  }

  @Test
  @DisplayName("Should apply pagination parameters correctly")
  void findMyInventoriesHistoryPagination() {
    Integer userId = 1;
    Integer departmentId = 10;

    Integer skip = 20;
    Integer take = 10;

    FindMyInventoriesHistoryInputDTO input =
        new FindMyInventoriesHistoryInputDTO(
            userId,
            null,
            null,
            skip,
            take
        );

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    when(getUserInfosService.getDepartment(userId))
        .thenReturn(department);

    when(inventoryRepository.findWithPagination(
        any(Specification.class),
        eq(skip),
        eq(take)
    )).thenReturn(List.of());

    when(inventoryRepository.count(any(Specification.class)))
        .thenReturn(0L);

    inventoryService.findMyInventoriesHistory(input);

    verify(inventoryRepository).findWithPagination(
        any(Specification.class),
        eq(skip),
        eq(take)
    );
  }

  @Test
  @DisplayName("Should count inventories using the same specification")
  void findMyInventoriesHistoryCount() {
    Integer userId = 1;
    Integer departmentId = 10;

    FindMyInventoriesHistoryInputDTO input =
        new FindMyInventoriesHistoryInputDTO(
            userId,
            null,
            null,
            0,
            10
        );

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    when(getUserInfosService.getDepartment(userId))
        .thenReturn(department);

    when(inventoryRepository.findWithPagination(
        any(Specification.class),
        any(),
        any()
    )).thenReturn(List.of());

    when(inventoryRepository.count(any(Specification.class)))
        .thenReturn(5L);

    FindMyInventoriesHistoryOutputDTO output =
        inventoryService.findMyInventoriesHistory(input);

    assertEquals(5L, output.totalCount());

    verify(inventoryRepository).count(any(Specification.class));
  }

  @Test
  @DisplayName("Should not query inventories when the department lookup fails")
  void findMyInventoriesHistoryWhenDepartmentLookupFails() {
    Integer userId = 1;

    FindMyInventoriesHistoryInputDTO input =
        new FindMyInventoriesHistoryInputDTO(
            userId,
            null,
            null,
            0,
            10
        );

    when(getUserInfosService.getDepartment(userId))
        .thenThrow(new RuntimeException("Department not found"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.findMyInventoriesHistory(input)
    );

    verifyNoInteractions(inventoryRepository);
  }

  // ---------------------------------------------------------------------------
  // registerInventory
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should register an inventory together with its storage file")
  void registerInventory() {
    Integer userId = 1;

    RegisterInventoryInputDTO input =
        new RegisterInventoryInputDTO(
            userId,
            "Inventário Teste",
            "arquivo.csv",
            "/uploads/arquivo.csv"
        );

    EmployeeEntity employee = mock(EmployeeEntity.class);

    when(getUserInfosService.getEmployee(userId))
        .thenReturn(employee);

    RegisterInventoryOutputDTO output =
        inventoryService.registerInventory(input);

    ArgumentCaptor<StorageFileEntity> fileCaptor =
        ArgumentCaptor.forClass(StorageFileEntity.class);

    ArgumentCaptor<InventoryEntity> inventoryCaptor =
        ArgumentCaptor.forClass(InventoryEntity.class);

    InOrder inOrder = inOrder(storageFileRepository, inventoryRepository);
    inOrder.verify(storageFileRepository).save(fileCaptor.capture());
    inOrder.verify(inventoryRepository).save(inventoryCaptor.capture());

    verify(getUserInfosService).getEmployee(userId);

    StorageFileEntity savedFile = fileCaptor.getValue();
    assertEquals("arquivo.csv", savedFile.getName());
    assertEquals("/uploads/arquivo.csv", savedFile.getPath());

    InventoryEntity savedInventory = inventoryCaptor.getValue();
    assertEquals("Inventário Teste", savedInventory.getName());
    assertEquals(InventoryTypeEnum.INPUT, savedInventory.getType());
    assertSame(employee, savedInventory.getOwnerEmployee());

    assertEquals("Inventário Teste", output.name());
    assertEquals(InventoryTypeEnum.INPUT, output.type());
  }

  @Test
  @DisplayName("Should not save anything when the employee lookup fails")
  void registerInventoryWhenEmployeeLookupFails() {
    Integer userId = 1;

    RegisterInventoryInputDTO input =
        new RegisterInventoryInputDTO(
            userId,
            "Inventário Teste",
            "arquivo.csv",
            "/uploads/arquivo.csv"
        );

    when(getUserInfosService.getEmployee(userId))
        .thenThrow(new RuntimeException("Employee not found"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.registerInventory(input)
    );

    verifyNoInteractions(storageFileRepository, inventoryRepository);
  }

  @Test
  @DisplayName("Should not save the inventory when saving the storage file fails")
  void registerInventoryWhenStorageFileSaveFails() {
    Integer userId = 1;

    RegisterInventoryInputDTO input =
        new RegisterInventoryInputDTO(
            userId,
            "Inventário Teste",
            "arquivo.csv",
            "/uploads/arquivo.csv"
        );

    when(getUserInfosService.getEmployee(userId))
        .thenReturn(mock(EmployeeEntity.class));

    when(storageFileRepository.save(any(StorageFileEntity.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.registerInventory(input)
    );

    verifyNoInteractions(inventoryRepository);
  }
}