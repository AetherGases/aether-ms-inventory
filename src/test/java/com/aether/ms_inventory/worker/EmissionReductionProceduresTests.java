package com.aether.ms_inventory.worker;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.CategoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmissionEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.GasEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.ReductionEntity;
import com.aether.ms_inventory.shared.persistence.postgres.entities.ScopeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.CategoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.EmissionRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.GasRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.InventoryRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.ReductionRepository;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.ScopeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@DisplayName("Emission/Reduction stored procedures Tests")
class EmissionReductionProceduresTests {

  private static final String ORIGIN = "MS_INVENTORY_WORKER";

  @Autowired private EmissionRepository emissionRepository;
  @Autowired private ReductionRepository reductionRepository;
  @Autowired private InventoryRepository inventoryRepository;
  @Autowired private GasRepository gasRepository;
  @Autowired private ScopeRepository scopeRepository;
  @Autowired private CategoryRepository categoryRepository;

  private InventoryEntity inventory;
  private GasEntity gas;
  private ScopeEntity scope;
  private CategoryEntity category;

  @BeforeEach
  void setUp() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);

    inventory = new InventoryEntity();
    inventory.setName("Procedure test " + suffix);
    inventory.setStatus(InventoryStatusEnum.PROCESSING);
    inventory.setType(InventoryTypeEnum.INPUT);
    inventory = inventoryRepository.saveAndFlush(inventory);

    gas = new GasEntity();
    gas.setName("Gas " + suffix);
    gas.setFormula("G" + suffix);
    gas.setBiogenic(false);
    gas.setGwp(BigDecimal.ONE);
    gas = gasRepository.saveAndFlush(gas);

    scope = new ScopeEntity();
    scope.setName("Scope " + suffix);
    scope = scopeRepository.saveAndFlush(scope);

    category = new CategoryEntity();
    category.setName("Category " + suffix);
    category = categoryRepository.saveAndFlush(category);
  }

  @Test
  @DisplayName("Should register an emission through sp_register_emission and return its id")
  void registerEmission() {
    Integer id = emissionRepository.registerEmission(
        new BigDecimal("12.5"), inventory.getId(), gas.getId(), scope.getId(), null,
        "Fator GHG Protocol", new BigDecimal("40"), "7", ORIGIN
    );

    assertNotNull(id);
    EmissionEntity saved = emissionRepository.findById(id).orElseThrow();
    assertEquals(0, new BigDecimal("12.5").compareTo(saved.getQuantityCo2e()));
    assertEquals(gas.getId(), saved.getGas().getId());
    assertNull(saved.getCategory());
  }

  @Test
  @DisplayName("Should register a reduction through sp_register_reduction and return its id")
  void registerReduction() {
    Integer id = reductionRepository.registerReduction(
        new BigDecimal("3"), inventory.getId(), category.getId(), "7", ORIGIN
    );

    assertNotNull(id);
    ReductionEntity saved = reductionRepository.findById(id).orElseThrow();
    assertEquals(category.getId(), saved.getCategory().getId());
  }

  @Test
  @DisplayName("Should propagate the procedure validation error when the inventory does not exist")
  void rejectsUnknownInventory() {
    assertThrows(DataAccessException.class, () -> reductionRepository.registerReduction(
        new BigDecimal("3"), Integer.MAX_VALUE, category.getId(), null, ORIGIN
    ));
  }
}
