package com.aether.ms_inventory.shared.persistence.postgres.repositories.custom;

import com.aether.ms_inventory.shared.workers.contexts.InventoryOutputContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class GhgDeclarationRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @SuppressWarnings("unchecked")
  public List<InventoryOutputContext.DeclarationCellData> findCompleteDeclaration(Integer enterpriseId) {
    List<Object[]> rows = entityManager
        .createNativeQuery("SELECT quadro, eixo, codigo, valor FROM fn_ghg_complete_declaration(:enterpriseId)")
        .setParameter("enterpriseId", enterpriseId)
        .getResultList();

    return rows.stream()
        .map(row -> new InventoryOutputContext.DeclarationCellData(
            (String) row[0],
            (String) row[1],
            (String) row[2],
            (BigDecimal) row[3]
        ))
        .toList();
  }
}
