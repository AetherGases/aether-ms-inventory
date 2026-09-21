package com.aether.ms_inventory.inventory;

import com.aether.ms_inventory.inventory.dto.input.FindMyInventoriesHistoryInputDTO;
import com.aether.ms_inventory.inventory.dto.output.FindMyInventoriesHistoryOutputDTO;
import com.aether.ms_inventory.inventory.services.InventoryService;
import com.aether.ms_inventory.shared.persistence.postgres.entities.DepartmentEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.services.GetUserInfosService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Tests")
public class InventoryServiceTests {

  @Mock
  private InventoryRepository inventoryRepository;

  @Mock
  private GetUserInfosService getUserInfosService;

  @InjectMocks
  private InventoryService inventoryService;

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
}