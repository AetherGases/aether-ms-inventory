package com.aether.ms_inventory.inventory;

import com.aether.ms_inventory.inventory.dto.input.*;
import com.aether.ms_inventory.inventory.dto.output.*;
import com.aether.ms_inventory.inventory.services.InventoryService;
import com.aether.ms_inventory.shared.enums.CategoryClassificationEnum;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.exceptions.NotFoundException;
import com.aether.ms_inventory.shared.persistence.postgres.entities.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            "Participação acionária",
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
    assertEquals("Participação acionária", savedInventory.getConsolidationApproach());
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
            "Participação acionária",
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
            "Participação acionária",
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
            "Participação acionária",
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
            "Participação acionária",
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

    when(inventoryRepository.findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW,
        validatorId
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
    verify(inventoryRepository).findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW,
        validatorId
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

    when(inventoryRepository.findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW,
        validatorId
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

    when(inventoryRepository.findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        any(),
        any(),
        any(),
        any()
    )).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> inventoryService.rejectInventory(input)
    );

    verify(inventoryRepository).findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        eq(inventoryId),
        eq(departmentId),
        eq(InventoryStatusEnum.UNDER_REVIEW),
        eq(validatorId)
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

    when(inventoryRepository.findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW,
        validatorId
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

    when(inventoryRepository.findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW,
        validatorId
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
    verify(inventoryRepository).findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW,
        validatorId
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

    when(inventoryRepository.findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW,
        validatorId
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

    when(inventoryRepository.findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        any(),
        any(),
        any(),
        any()
    )).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> inventoryService.approveInventory(input)
    );

    verify(inventoryRepository).findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        eq(inventoryId),
        eq(departmentId),
        eq(InventoryStatusEnum.UNDER_REVIEW),
        eq(validatorId)
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

    when(inventoryRepository.findByIdAndDepartmentIdAndStatusAndOwnerEmployeeIdNot(
        inventoryId,
        departmentId,
        InventoryStatusEnum.UNDER_REVIEW,
        validatorId
    )).thenReturn(Optional.of(inventory));

    when(inventoryRepository.save(any(InventoryEntity.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.approveInventory(input)
    );

    verify(inventoryRepository).save(any(InventoryEntity.class));
  }

  // ---------------------------------------------------------------------------
  // findManyReviewedInventories
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return reviewed inventories with pagination")
  void findManyReviewedInventories() {
    Integer userId = 1;
    Integer departmentId = 10;

    FindManyReviewedInventoriesInputDTO input =
        new FindManyReviewedInventoriesInputDTO(
            userId,
            null,
            null,
            0,
            10
        );

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    EmployeeEntity employee = mock(EmployeeEntity.class);

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
    inventory.setStatus(InventoryStatusEnum.APPROVED);

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

    FindManyReviewedInventoriesOutputDTO output =
        inventoryService.findManyReviewedInventories(input);

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
  @DisplayName("Should return an empty list when there are no reviewed inventories")
  void findManyReviewedInventoriesEmpty() {
    Integer userId = 1;
    Integer departmentId = 10;

    FindManyReviewedInventoriesInputDTO input =
        new FindManyReviewedInventoriesInputDTO(
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

    FindManyReviewedInventoriesOutputDTO output =
        inventoryService.findManyReviewedInventories(input);

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
  void findManyReviewedInventoriesPagination() {
    Integer userId = 1;
    Integer departmentId = 10;

    Integer skip = 20;
    Integer take = 10;

    FindManyReviewedInventoriesInputDTO input =
        new FindManyReviewedInventoriesInputDTO(
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

    inventoryService.findManyReviewedInventories(input);

    verify(inventoryRepository).findWithPagination(
        any(Specification.class),
        eq(skip),
        eq(take)
    );
  }

  @Test
  @DisplayName("Should count reviewed inventories using the same specification")
  void findManyReviewedInventoriesCount() {
    Integer userId = 1;
    Integer departmentId = 10;

    FindManyReviewedInventoriesInputDTO input =
        new FindManyReviewedInventoriesInputDTO(
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
        .thenReturn(7L);

    FindManyReviewedInventoriesOutputDTO output =
        inventoryService.findManyReviewedInventories(input);

    assertEquals(7L, output.totalCount());

    verify(inventoryRepository).count(any(Specification.class));
  }

  @Test
  @DisplayName("Should not query inventories when the department lookup fails")
  void findManyReviewedInventoriesWhenDepartmentLookupFails() {
    Integer userId = 1;

    FindManyReviewedInventoriesInputDTO input =
        new FindManyReviewedInventoriesInputDTO(
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
        () -> inventoryService.findManyReviewedInventories(input)
    );

    verifyNoInteractions(inventoryRepository);
  }

  // ---------------------------------------------------------------------------
  // countInventories
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return the reviewed and submitted inventory counts")
  void countInventories() {
    Integer userId = 1;

    CountInventoriesInputDTO input = new CountInventoriesInputDTO(userId);

    when(inventoryRepository.count(any(Specification.class)))
        .thenReturn(3L)
        .thenReturn(8L);

    CountInventoriesOutputDTO output = inventoryService.countInventories(input);

    assertEquals(3L, output.reviewedCount());
    assertEquals(8L, output.submittedCount());

    verify(inventoryRepository, times(2)).count(any(Specification.class));
    verifyNoInteractions(getUserInfosService);
  }

  @Test
  @DisplayName("Should return zero counts when there are no inventories")
  void countInventoriesEmpty() {
    Integer userId = 1;

    CountInventoriesInputDTO input = new CountInventoriesInputDTO(userId);

    when(inventoryRepository.count(any(Specification.class)))
        .thenReturn(0L);

    CountInventoriesOutputDTO output = inventoryService.countInventories(input);

    assertEquals(0L, output.reviewedCount());
    assertEquals(0L, output.submittedCount());
  }

  // ---------------------------------------------------------------------------
  // findMineInventoryById
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return the inventory details when it belongs to the requesting user")
  void findMineInventoryById() {
    Integer inventoryId = 5;
    Integer userId = 1;

    FindInventoryByIdInputDTO input = new FindInventoryByIdInputDTO(inventoryId, userId);

    InventoryEntity inventory = buildDetailedInventory(inventoryId, InventoryStatusEnum.APPROVED);

    when(inventoryRepository.findByIdAndOwnerEmployeeId(inventoryId, userId))
        .thenReturn(Optional.of(inventory));

    FindInventoryByIdOutputDTO output = inventoryService.findMineInventoryById(input);

    assertCompleteInventoryDetails(output, inventoryId, InventoryStatusEnum.APPROVED);

    verify(inventoryRepository).findByIdAndOwnerEmployeeId(inventoryId, userId);
    verifyNoInteractions(getUserInfosService);
  }

  @Test
  @DisplayName("Should throw NotFoundException when the inventory does not belong to the user")
  void findMineInventoryByIdNotFound() {
    Integer inventoryId = 5;
    Integer userId = 1;

    FindInventoryByIdInputDTO input = new FindInventoryByIdInputDTO(inventoryId, userId);

    when(inventoryRepository.findByIdAndOwnerEmployeeId(inventoryId, userId))
        .thenReturn(Optional.empty());

    NotFoundException exception = assertThrows(
        NotFoundException.class,
        () -> inventoryService.findMineInventoryById(input)
    );

    assertEquals("exception.inventory.not-found", exception.getMessage());
  }

  @Test
  @DisplayName("Should look up the inventory using the given id and owner employee id")
  void findMineInventoryByIdSearchesByIdAndOwner() {
    Integer inventoryId = 5;
    Integer userId = 1;

    FindInventoryByIdInputDTO input = new FindInventoryByIdInputDTO(inventoryId, userId);

    when(inventoryRepository.findByIdAndOwnerEmployeeId(any(), any()))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> inventoryService.findMineInventoryById(input)
    );

    verify(inventoryRepository).findByIdAndOwnerEmployeeId(eq(inventoryId), eq(userId));
  }

  // ---------------------------------------------------------------------------
  // findReviewedInventoryById
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return the inventory details when it belongs to the user's department")
  void findReviewedInventoryById() {
    Integer inventoryId = 5;
    Integer userId = 1;
    Integer departmentId = 10;

    FindInventoryByIdInputDTO input = new FindInventoryByIdInputDTO(inventoryId, userId);

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    InventoryEntity inventory = buildDetailedInventory(inventoryId, InventoryStatusEnum.APPROVED);

    when(getUserInfosService.getDepartment(userId))
        .thenReturn(department);

    when(inventoryRepository.findByIdAndDepartmentId(inventoryId, departmentId))
        .thenReturn(Optional.of(inventory));

    FindInventoryByIdOutputDTO output = inventoryService.findReviewedInventoryById(input);

    assertCompleteInventoryDetails(output, inventoryId, InventoryStatusEnum.APPROVED);

    verify(getUserInfosService).getDepartment(userId);
    verify(inventoryRepository).findByIdAndDepartmentId(inventoryId, departmentId);
  }

  @Test
  @DisplayName("Should throw NotFoundException when the inventory is not in the user's department")
  void findReviewedInventoryByIdNotFound() {
    Integer inventoryId = 5;
    Integer userId = 1;
    Integer departmentId = 10;

    FindInventoryByIdInputDTO input = new FindInventoryByIdInputDTO(inventoryId, userId);

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    when(getUserInfosService.getDepartment(userId))
        .thenReturn(department);

    when(inventoryRepository.findByIdAndDepartmentId(inventoryId, departmentId))
        .thenReturn(Optional.empty());

    NotFoundException exception = assertThrows(
        NotFoundException.class,
        () -> inventoryService.findReviewedInventoryById(input)
    );

    assertEquals("exception.inventory.not-found", exception.getMessage());
  }

  @Test
  @DisplayName("Should not query inventories when the department lookup fails")
  void findReviewedInventoryByIdWhenDepartmentLookupFails() {
    Integer userId = 1;

    FindInventoryByIdInputDTO input = new FindInventoryByIdInputDTO(5, userId);

    when(getUserInfosService.getDepartment(userId))
        .thenThrow(new RuntimeException("Department not found"));

    assertThrows(
        RuntimeException.class,
        () -> inventoryService.findReviewedInventoryById(input)
    );

    verifyNoInteractions(inventoryRepository);
  }

  @Test
  @DisplayName("Should return empty emissions and reductions and no validator when the reviewed inventory has none")
  void findReviewedInventoryByIdWithoutEmissionsReductionsOrValidator() {
    Integer inventoryId = 5;
    Integer userId = 1;
    Integer departmentId = 10;

    FindInventoryByIdInputDTO input = new FindInventoryByIdInputDTO(inventoryId, userId);

    DepartmentEntity department = new DepartmentEntity();
    department.setId(departmentId);

    InventoryEntity inventory = new InventoryEntity();
    inventory.setId(inventoryId);
    inventory.setName("Inventário Teste");
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);
    inventory.setType(InventoryTypeEnum.INPUT);

    when(getUserInfosService.getDepartment(userId))
        .thenReturn(department);

    when(inventoryRepository.findByIdAndDepartmentId(inventoryId, departmentId))
        .thenReturn(Optional.of(inventory));

    FindInventoryByIdOutputDTO output = inventoryService.findReviewedInventoryById(input);

    assertNull(output.validator());
    assertTrue(output.emissions().isEmpty());
    assertTrue(output.reductions().isEmpty());
  }

  @Test
  @DisplayName("Should return null nested data when an emission has no scope, category or gas")
  void findMineInventoryByIdWithEmissionMissingRelations() {
    Integer inventoryId = 5;
    Integer userId = 1;

    FindInventoryByIdInputDTO input = new FindInventoryByIdInputDTO(inventoryId, userId);

    EmissionEntity emission = new EmissionEntity();
    emission.setQuantityCo2e(new BigDecimal("1.5"));

    InventoryEntity inventory = new InventoryEntity();
    inventory.setId(inventoryId);
    inventory.setName("Inventário Teste");
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);
    inventory.setType(InventoryTypeEnum.INPUT);
    inventory.setEmissions(List.of(emission));
    inventory.setReductions(List.of());

    when(inventoryRepository.findByIdAndOwnerEmployeeId(inventoryId, userId))
        .thenReturn(Optional.of(inventory));

    FindInventoryByIdOutputDTO output = inventoryService.findMineInventoryById(input);

    assertEquals(1, output.emissions().size());
    FindInventoryByIdOutputDTO.Emission outputEmission = output.emissions().getFirst();
    assertEquals(new BigDecimal("1.5"), outputEmission.quantityCo2e());
    assertNull(outputEmission.scope());
    assertNull(outputEmission.category());
    assertNull(outputEmission.gas());
    assertFalse(outputEmission.isReduction());
  }

  private InventoryEntity buildDetailedInventory(Integer inventoryId, InventoryStatusEnum status) {
    EmployeeEntity owner = new EmployeeEntity();
    owner.setId(1);
    owner.setName("autor teste");
    owner.setCpf("12345678912");

    EmployeeEntity validator = new EmployeeEntity();
    validator.setId(2);
    validator.setName("validador teste");
    validator.setCpf("52998224725");

    ScopeEntity scope = new ScopeEntity();
    scope.setId(1);
    scope.setName("Escopo 1");

    CategoryEntity emissionCategory = new CategoryEntity();
    emissionCategory.setId(2);
    emissionCategory.setName("Combustão móvel");
    emissionCategory.setClassification(CategoryClassificationEnum.UPSTREAM);

    CategoryEntity reductionCategory = new CategoryEntity();
    reductionCategory.setId(3);
    reductionCategory.setName("Resíduos");
    reductionCategory.setClassification(CategoryClassificationEnum.DOWNSTREAM);

    GasEntity gas = new GasEntity();
    gas.setId(4);
    gas.setName("Metano");
    gas.setFormula("CH4");
    gas.setBiogenic(false);
    gas.setGwp(new BigDecimal("28"));

    EmissionEntity emission = new EmissionEntity();
    emission.setId(10);
    emission.setQuantityCo2e(new BigDecimal("32.16"));
    emission.setMethodologyDescription("Combustion of fuel in owned fleet");
    emission.setSupplierDataPercentage(new BigDecimal("40"));
    emission.setScope(scope);
    emission.setCategory(emissionCategory);
    emission.setGas(gas);

    ReductionEntity reduction = new ReductionEntity();
    reduction.setId(20);
    reduction.setQuantityCo2e(new BigDecimal("10.5"));
    reduction.setCategory(reductionCategory);

    InventoryEntity inventory = new InventoryEntity();
    inventory.setId(inventoryId);
    inventory.setName("inventário teste");
    inventory.setDescription("Inventário anual de emissões");
    inventory.setConsolidationApproach("Controle operacional");
    inventory.setInventoryPeriodStart(LocalDate.of(2025, 1, 1));
    inventory.setInventoryPeriodEnd(LocalDate.of(2025, 12, 31));
    inventory.setStatusMessage("Inventário validado");
    inventory.setStatus(status);
    inventory.setType(InventoryTypeEnum.INPUT);
    inventory.setOwnerEmployee(owner);
    inventory.setValidatorEmployee(validator);
    inventory.setEmissions(List.of(emission));
    inventory.setReductions(List.of(reduction));

    return inventory;
  }

  private void assertCompleteInventoryDetails(
      FindInventoryByIdOutputDTO output,
      Integer inventoryId,
      InventoryStatusEnum status
  ) {
    // general information
    assertEquals(inventoryId, output.id());
    assertEquals("Inventário Teste", output.name());
    assertEquals("Inventário anual de emissões", output.description());
    assertEquals("Controle operacional", output.consolidationApproach());
    assertEquals(LocalDate.of(2025, 1, 1), output.inventoryPeriodStart());
    assertEquals(LocalDate.of(2025, 12, 31), output.inventoryPeriodEnd());
    assertEquals(InventoryTypeEnum.INPUT, output.type());
    assertEquals(status, output.status());
    assertEquals("Inventário validado", output.validationMessage());
    assertEquals(1, output.owner().id());
    assertEquals("Autor Teste", output.owner().name());

    // validator
    assertEquals(2, output.validator().id());
    assertEquals("Validador Teste", output.validator().name());
    assertEquals("529.982.247-25", output.validator().cpf());

    // emissions
    assertEquals(1, output.emissions().size());
    FindInventoryByIdOutputDTO.Emission emission = output.emissions().getFirst();
    assertEquals(10, emission.id());
    assertEquals(new BigDecimal("32.16"), emission.quantityCo2e());
    assertEquals("Combustion of fuel in owned fleet", emission.methodologyDescription());
    assertEquals(new BigDecimal("40"), emission.supplierDataPercentage());
    assertFalse(emission.isReduction());

    // emission categorization
    assertEquals(new FindInventoryByIdOutputDTO.Scope(1, "Escopo 1"), emission.scope());
    assertEquals(
        new FindInventoryByIdOutputDTO.Category(2, "Combustão móvel", CategoryClassificationEnum.UPSTREAM),
        emission.category()
    );

    // gas
    assertEquals(
        new FindInventoryByIdOutputDTO.Gas(4, "Metano", "CH4", false, new BigDecimal("28")),
        emission.gas()
    );

    // reductions
    assertEquals(1, output.reductions().size());
    FindInventoryByIdOutputDTO.Reduction reduction = output.reductions().getFirst();
    assertEquals(20, reduction.id());
    assertEquals(new BigDecimal("10.5"), reduction.quantityCo2e());
    assertTrue(reduction.isReduction());
    assertEquals(
        new FindInventoryByIdOutputDTO.Category(3, "Resíduos", CategoryClassificationEnum.DOWNSTREAM),
        reduction.category()
    );
  }

  // ---------------------------------------------------------------------------
  // findMyLastInventory
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return the owner's last input inventory")
  void findMyLastInventory() {
    Integer userId = 1;

    FindLastInventoryInputDTO input = new FindLastInventoryInputDTO(userId);

    EmployeeEntity employee = mock(EmployeeEntity.class);
    when(employee.getName()).thenReturn("Autor Teste");
    when(employee.getStorageFile()).thenReturn(null);

    StorageFileEntity storageFile = new StorageFileEntity(
        "arquivo.xlsx",
        "/uploads/arquivo.xlsx"
    );

    InventoryEntity inventory = new InventoryEntity(
        "Inventário Teste",
        InventoryTypeEnum.INPUT,
        employee,
        storageFile
    );
    inventory.setId(5);
    inventory.setStatus(InventoryStatusEnum.UNDER_REVIEW);
    inventory.setInventoryPeriodStart(LocalDate.of(2026, 1, 1));
    inventory.setInventoryPeriodEnd(LocalDate.of(2026, 1, 31));
    inventory.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
    inventory.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));

    when(inventoryRepository.findWithPagination(
        any(Specification.class),
        eq(0),
        eq(1)
    )).thenReturn(List.of(inventory));

    LastInventoryOutputDTO output = inventoryService.findMyLastInventory(input);

    assertEquals(5, output.id());
    assertEquals("Inventário Teste", output.name());
    assertEquals("Autor Teste", output.author().name());
    assertNull(output.author().imageUrl());
    assertEquals(LocalDate.of(2026, 1, 1), output.period().start());
    assertEquals(LocalDate.of(2026, 1, 31), output.period().end());
    assertEquals(InventoryStatusEnum.UNDER_REVIEW, output.status());
    assertEquals(InventoryTypeEnum.INPUT, output.type());
    assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), output.createdAt());
    assertEquals(LocalDateTime.of(2026, 1, 2, 10, 0), output.updatedAt());

    verify(inventoryRepository).findWithPagination(
        any(Specification.class),
        eq(0),
        eq(1)
    );

    verifyNoInteractions(getUserInfosService);
  }

  @Test
  @DisplayName("Should throw NotFoundException when the owner has no input inventories")
  void findMyLastInventoryNotFound() {
    Integer userId = 1;

    FindLastInventoryInputDTO input = new FindLastInventoryInputDTO(userId);

    when(inventoryRepository.findWithPagination(
        any(Specification.class),
        eq(0),
        eq(1)
    )).thenReturn(List.of());

    NotFoundException exception = assertThrows(
        NotFoundException.class,
        () -> inventoryService.findMyLastInventory(input)
    );

    assertEquals("exception.last-mine-inventory.not-found", exception.getMessage());
  }

  @Test
  @DisplayName("Should request only the single most recent inventory")
  void findMyLastInventoryRequestsOnlyOneResult() {
    Integer userId = 1;

    FindLastInventoryInputDTO input = new FindLastInventoryInputDTO(userId);

    when(inventoryRepository.findWithPagination(
        any(Specification.class),
        any(),
        any()
    )).thenReturn(List.of());

    assertThrows(
        NotFoundException.class,
        () -> inventoryService.findMyLastInventory(input)
    );

    verify(inventoryRepository).findWithPagination(
        any(Specification.class),
        eq(0),
        eq(1)
    );
  }

  // ---------------------------------------------------------------------------
  // findReviewedLastInventory
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return the validator's last reviewed inventory")
  void findReviewedLastInventory() {
    Integer userId = 1;

    FindLastInventoryInputDTO input = new FindLastInventoryInputDTO(userId);

    EmployeeEntity employee = mock(EmployeeEntity.class);
    when(employee.getName()).thenReturn("Autor Teste");
    when(employee.getStorageFile()).thenReturn(null);

    StorageFileEntity storageFile = new StorageFileEntity(
        "arquivo.xlsx",
        "/uploads/arquivo.xlsx"
    );

    InventoryEntity inventory = new InventoryEntity(
        "Inventário Revisado",
        InventoryTypeEnum.INPUT,
        employee,
        storageFile
    );
    inventory.setId(7);
    inventory.setStatus(InventoryStatusEnum.APPROVED);

    when(inventoryRepository.findWithPagination(
        any(Specification.class),
        eq(0),
        eq(1)
    )).thenReturn(List.of(inventory));

    LastInventoryOutputDTO output = inventoryService.findReviewedLastInventory(input);

    assertEquals(7, output.id());
    assertEquals("Inventário Revisado", output.name());
    assertEquals("Autor Teste", output.author().name());
    assertEquals(InventoryStatusEnum.APPROVED, output.status());

    verify(inventoryRepository).findWithPagination(
        any(Specification.class),
        eq(0),
        eq(1)
    );

    verifyNoInteractions(getUserInfosService);
  }

  @Test
  @DisplayName("Should throw NotFoundException when the validator has no reviewed inventories")
  void findReviewedLastInventoryNotFound() {
    Integer userId = 1;

    FindLastInventoryInputDTO input = new FindLastInventoryInputDTO(userId);

    when(inventoryRepository.findWithPagination(
        any(Specification.class),
        eq(0),
        eq(1)
    )).thenReturn(List.of());

    NotFoundException exception = assertThrows(
        NotFoundException.class,
        () -> inventoryService.findReviewedLastInventory(input)
    );

    assertEquals("exception.last-reviewed-inventory.not-found", exception.getMessage());
  }

  @Test
  @DisplayName("Should request only the single most recent reviewed inventory")
  void findReviewedLastInventoryRequestsOnlyOneResult() {
    Integer userId = 1;

    FindLastInventoryInputDTO input = new FindLastInventoryInputDTO(userId);

    when(inventoryRepository.findWithPagination(
        any(Specification.class),
        any(),
        any()
    )).thenReturn(List.of());

    assertThrows(
        NotFoundException.class,
        () -> inventoryService.findReviewedLastInventory(input)
    );

    verify(inventoryRepository).findWithPagination(
        any(Specification.class),
        eq(0),
        eq(1)
    );
  }
}