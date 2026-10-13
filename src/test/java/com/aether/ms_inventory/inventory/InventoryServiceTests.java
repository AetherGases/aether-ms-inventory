package com.aether.ms_inventory.inventory;

import com.aether.ms_inventory.inventory.dto.input.*;
import com.aether.ms_inventory.inventory.dto.output.*;
import com.aether.ms_inventory.inventory.services.InventoryService;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.exceptions.NotFoundException;
import com.aether.ms_inventory.shared.persistence.postgres.entities.DepartmentEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.StorageFileEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.StorageFileRepository;
import com.aether.ms_inventory.shared.persistence.redis.entities.InputInventoryDocument;
import com.aether.ms_inventory.shared.persistence.redis.repositories.InputInventoryRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Tests")
public class InventoryServiceTests {

  @Mock
  private InventoryRepository inventoryRepository;

  @Mock
  private StorageFileRepository storageFileRepository;
  @Mock
  private InputInventoryRepository inputInventoryRepository;

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
  @DisplayName("Should register an inventory together with its storage file and redis document")
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

    ArgumentCaptor<InputInventoryDocument> documentCaptor =
        ArgumentCaptor.forClass(InputInventoryDocument.class);

    InOrder inOrder = inOrder(
        storageFileRepository,
        inventoryRepository,
        inputInventoryRepository
    );
    inOrder.verify(storageFileRepository).save(fileCaptor.capture());
    inOrder.verify(inventoryRepository).save(inventoryCaptor.capture());
    inOrder.verify(inputInventoryRepository).save(documentCaptor.capture());

    verify(getUserInfosService).getEmployee(userId);

    StorageFileEntity savedFile = fileCaptor.getValue();
    assertEquals("arquivo.csv", savedFile.getName());
    assertEquals("/uploads/arquivo.csv", savedFile.getPath());

    InventoryEntity savedInventory = inventoryCaptor.getValue();
    assertEquals("Inventário Teste", savedInventory.getName());
    assertEquals(InventoryTypeEnum.INPUT, savedInventory.getType());
    assertSame(employee, savedInventory.getOwnerEmployee());

    InputInventoryDocument savedDocument = documentCaptor.getValue();
    assertEquals(savedInventory.getId(), savedDocument.getId());
    assertEquals(savedInventory.getStatus(), savedDocument.getStatus());

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

    verifyNoInteractions(
        storageFileRepository,
        inventoryRepository,
        inputInventoryRepository
    );
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

    verifyNoInteractions(inventoryRepository, inputInventoryRepository);
  }

  @Test
  @DisplayName("Should not save the redis document when saving the inventory fails")
  void registerInventoryWhenInventorySaveFails() {
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

    when(inventoryRepository.save(any(InventoryEntity.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.registerInventory(input)
    );

    verify(storageFileRepository).save(any(StorageFileEntity.class));
    verifyNoInteractions(inputInventoryRepository);
  }

  @Test
  @DisplayName("Should propagate the error when saving the redis document fails")
  void registerInventoryWhenRedisSaveFails() {
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

    when(inputInventoryRepository.save(any(InputInventoryDocument.class)))
        .thenThrow(new RuntimeException("Redis error"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.registerInventory(input)
    );

    verify(storageFileRepository).save(any(StorageFileEntity.class));
    verify(inventoryRepository).save(any(InventoryEntity.class));
  }

  // ---------------------------------------------------------------------------
  // findManyPendingReports
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return pending inventories with pagination")
  void findManyPendingReports() {
    Integer userId = 1;
    Integer departmentId = 10;

    FindManyPendingInventoriesInputDTO input =
        new FindManyPendingInventoriesInputDTO(
            userId,
            null,
            0,
            10
        );

    EmployeeEntity employee = mock(EmployeeEntity.class);

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    when(employee.getDepartment()).thenReturn(department);

    StorageFileEntity storageFile = new StorageFileEntity(
        "arquivo.csv",
        "/uploads/arquivo.csv"
    );

    InventoryEntity inventory = new InventoryEntity(
        "Inventário Teste",
        InventoryTypeEnum.INPUT,
        employee,
        storageFile
    );

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

    FindManyPendingInventoriesOutputDTO output =
        inventoryService.findManyPendingReports(input);

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
  @DisplayName("Should return an empty list when there are no pending inventories")
  void findManyPendingReportsEmpty() {
    Integer userId = 1;
    Integer departmentId = 10;

    FindManyPendingInventoriesInputDTO input =
        new FindManyPendingInventoriesInputDTO(
            userId,
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

    FindManyPendingInventoriesOutputDTO output =
        inventoryService.findManyPendingReports(input);

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
  void findManyPendingReportsPagination() {
    Integer userId = 1;
    Integer departmentId = 10;

    Integer skip = 20;
    Integer take = 10;

    FindManyPendingInventoriesInputDTO input =
        new FindManyPendingInventoriesInputDTO(
            userId,
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

    inventoryService.findManyPendingReports(input);

    verify(inventoryRepository).findWithPagination(
        any(Specification.class),
        eq(skip),
        eq(take)
    );
  }

  @Test
  @DisplayName("Should count pending inventories using the same specification")
  void findManyPendingReportsCount() {
    Integer userId = 1;
    Integer departmentId = 10;

    FindManyPendingInventoriesInputDTO input =
        new FindManyPendingInventoriesInputDTO(
            userId,
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

    FindManyPendingInventoriesOutputDTO output =
        inventoryService.findManyPendingReports(input);

    assertEquals(5L, output.totalCount());

    verify(inventoryRepository).count(any(Specification.class));
  }

  @Test
  @DisplayName("Should not query inventories when the department lookup fails")
  void findManyPendingReportsWhenDepartmentLookupFails() {
    Integer userId = 1;

    FindManyPendingInventoriesInputDTO input =
        new FindManyPendingInventoriesInputDTO(
            userId,
            null,
            0,
            10
        );

    when(getUserInfosService.getDepartment(userId))
        .thenThrow(new RuntimeException("Department not found"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.findManyPendingReports(input)
    );

    verifyNoInteractions(inventoryRepository);
  }

  // ---------------------------------------------------------------------------
  // rejectInventory
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should reject an inventory under review and persist the status message")
  void rejectInventory() {
    Integer inventoryId = 5;
    Integer validatorId = 1;
    Integer departmentId = 10;
    String message = "Arquivo com dados inconsistentes";

    RejectInventoryInputDTO input =
        new RejectInventoryInputDTO(inventoryId, validatorId, message);

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    InventoryEntity inventory = new InventoryEntity();
    inventory.setName("Inventário Teste");
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);

    when(getUserInfosService.getDepartment(validatorId))
        .thenReturn(department);

    when(inventoryRepository.findByIdAndDepartmentIdAndStatus(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW
    )).thenReturn(Optional.of(inventory));

    RejectInventoryOutputDTO output = inventoryService.rejectInventory(input);

    ArgumentCaptor<InventoryEntity> captor =
        ArgumentCaptor.forClass(InventoryEntity.class);

    verify(inventoryRepository).save(captor.capture());

    InventoryEntity saved = captor.getValue();
    assertSame(inventory, saved);
    assertEquals(InventoryStatusEnum.REJECTED, saved.getStatus());
    assertEquals(message, saved.getStatusMessage());

    assertNotNull(output);

    verify(getUserInfosService).getDepartment(validatorId);
    verify(inventoryRepository).findByIdAndDepartmentIdAndStatus(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW
    );
  }

  @Test
  @DisplayName("Should throw NotFoundException and not save when the inventory is not found")
  void rejectInventoryWhenInventoryNotFound() {
    Integer inventoryId = 5;
    Integer validatorId = 1;
    Integer departmentId = 10;

    RejectInventoryInputDTO input =
        new RejectInventoryInputDTO(inventoryId, validatorId, "Motivo");

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    when(getUserInfosService.getDepartment(validatorId))
        .thenReturn(department);

    when(inventoryRepository.findByIdAndDepartmentIdAndStatus(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW
    )).thenReturn(Optional.empty());

    NotFoundException exception = assertThrows(
        NotFoundException.class,
        () -> inventoryService.rejectInventory(input)
    );

    assertEquals("exception.inventory.not-found", exception.getMessage());

    verify(inventoryRepository, never()).save(any(InventoryEntity.class));
  }

  @Test
  @DisplayName("Should only look for inventories under review in the validator's department")
  void rejectInventorySearchesOnlyUnderReviewInValidatorDepartment() {
    Integer inventoryId = 5;
    Integer validatorId = 1;
    Integer departmentId = 10;

    RejectInventoryInputDTO input =
        new RejectInventoryInputDTO(inventoryId, validatorId, "Motivo");

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    when(getUserInfosService.getDepartment(validatorId))
        .thenReturn(department);

    when(inventoryRepository.findByIdAndDepartmentIdAndStatus(
        any(),
        any(),
        any()
    )).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> inventoryService.rejectInventory(input)
    );

    verify(inventoryRepository).findByIdAndDepartmentIdAndStatus(
        eq(inventoryId),
        eq(departmentId),
        eq(InventoryStatusEnum.UNDER_REVIEW)
    );
  }

  @Test
  @DisplayName("Should not query inventories when the department lookup fails")
  void rejectInventoryWhenDepartmentLookupFails() {
    Integer validatorId = 1;

    RejectInventoryInputDTO input =
        new RejectInventoryInputDTO(5, validatorId, "Motivo");

    when(getUserInfosService.getDepartment(validatorId))
        .thenThrow(new RuntimeException("Department not found"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.rejectInventory(input)
    );

    verifyNoInteractions(inventoryRepository);
  }

  @Test
  @DisplayName("Should propagate the error when saving the rejected inventory fails")
  void rejectInventoryWhenSaveFails() {
    Integer inventoryId = 5;
    Integer validatorId = 1;
    Integer departmentId = 10;

    RejectInventoryInputDTO input =
        new RejectInventoryInputDTO(inventoryId, validatorId, "Motivo");

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    InventoryEntity inventory = new InventoryEntity();
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);

    when(getUserInfosService.getDepartment(validatorId))
        .thenReturn(department);

    when(inventoryRepository.findByIdAndDepartmentIdAndStatus(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW
    )).thenReturn(Optional.of(inventory));

    when(inventoryRepository.save(any(InventoryEntity.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.rejectInventory(input)
    );

    verify(inventoryRepository).save(any(InventoryEntity.class));
  }

  // ---------------------------------------------------------------------------
  // approveInventory
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should approve an inventory under review and persist the status message")
  void approveInventory() {
    Integer inventoryId = 5;
    Integer validatorId = 1;
    Integer departmentId = 10;
    String message = "Dados conferidos";

    ApproveInventoryInputDTO input =
        new ApproveInventoryInputDTO(inventoryId, validatorId, message);

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    InventoryEntity inventory = new InventoryEntity();
    inventory.setName("Inventário Teste");
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);

    when(getUserInfosService.getDepartment(validatorId))
        .thenReturn(department);

    when(inventoryRepository.findByIdAndDepartmentIdAndStatus(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW
    )).thenReturn(Optional.of(inventory));

    ApproveInventoryOutputDTO output = inventoryService.approveInventory(input);

    ArgumentCaptor<InventoryEntity> captor =
        ArgumentCaptor.forClass(InventoryEntity.class);

    verify(inventoryRepository).save(captor.capture());

    InventoryEntity saved = captor.getValue();
    assertSame(inventory, saved);
    assertEquals(InventoryStatusEnum.APPROVED, saved.getStatus());
    assertEquals(message, saved.getStatusMessage());

    assertNotNull(output);

    verify(getUserInfosService).getDepartment(validatorId);
    verify(inventoryRepository).findByIdAndDepartmentIdAndStatus(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW
    );
  }

  @Test
  @DisplayName("Should throw NotFoundException and not save when the inventory is not found")
  void approveInventoryWhenInventoryNotFound() {
    Integer inventoryId = 5;
    Integer validatorId = 1;
    Integer departmentId = 10;

    ApproveInventoryInputDTO input =
        new ApproveInventoryInputDTO(inventoryId, validatorId, "Motivo");

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    when(getUserInfosService.getDepartment(validatorId))
        .thenReturn(department);

    when(inventoryRepository.findByIdAndDepartmentIdAndStatus(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW
    )).thenReturn(Optional.empty());

    NotFoundException exception = assertThrows(
        NotFoundException.class,
        () -> inventoryService.approveInventory(input)
    );

    assertEquals("exception.inventory.not-found", exception.getMessage());

    verify(inventoryRepository, never()).save(any(InventoryEntity.class));
  }

  @Test
  @DisplayName("Should only look for inventories under review in the validator's department")
  void approveInventorySearchesOnlyUnderReviewInValidatorDepartment() {
    Integer inventoryId = 5;
    Integer validatorId = 1;
    Integer departmentId = 10;

    ApproveInventoryInputDTO input =
        new ApproveInventoryInputDTO(inventoryId, validatorId, "Motivo");

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    when(getUserInfosService.getDepartment(validatorId))
        .thenReturn(department);

    when(inventoryRepository.findByIdAndDepartmentIdAndStatus(
        any(),
        any(),
        any()
    )).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> inventoryService.approveInventory(input)
    );

    verify(inventoryRepository).findByIdAndDepartmentIdAndStatus(
        eq(inventoryId),
        eq(departmentId),
        eq(InventoryStatusEnum.UNDER_REVIEW)
    );
  }

  @Test
  @DisplayName("Should not query inventories when the department lookup fails")
  void approveInventoryWhenDepartmentLookupFails() {
    Integer validatorId = 1;

    ApproveInventoryInputDTO input =
        new ApproveInventoryInputDTO(5, validatorId, "Motivo");

    when(getUserInfosService.getDepartment(validatorId))
        .thenThrow(new RuntimeException("Department not found"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.approveInventory(input)
    );

    verifyNoInteractions(inventoryRepository);
  }

  @Test
  @DisplayName("Should propagate the error when saving the approved inventory fails")
  void approveInventoryWhenSaveFails() {
    Integer inventoryId = 5;
    Integer validatorId = 1;
    Integer departmentId = 10;

    ApproveInventoryInputDTO input =
        new ApproveInventoryInputDTO(inventoryId, validatorId, "Motivo");

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    InventoryEntity inventory = new InventoryEntity();
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);

    when(getUserInfosService.getDepartment(validatorId))
        .thenReturn(department);

    when(inventoryRepository.findByIdAndDepartmentIdAndStatus(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW
    )).thenReturn(Optional.of(inventory));

    when(inventoryRepository.save(any(InventoryEntity.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.approveInventory(input)
    );

    verify(inventoryRepository).save(any(InventoryEntity.class));
  }
}