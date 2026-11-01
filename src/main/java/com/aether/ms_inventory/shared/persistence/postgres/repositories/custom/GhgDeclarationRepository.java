package com.aether.ms_inventory.shared.persistence.postgres.repositories.custom;

import com.aether.ms_inventory.shared.workers.InventoryOutputContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class GhgDeclarationRepository {

  @PersistenceContext
  private EntityManager entityManager;

  // fn_ghg_complete_declaration devolve as 84 células manuais dos quadros 1.1, 1.2, 1.3 e 1.5
  // (inventários APPROVED do ano civil anterior), com valor NULL quando não há emissão.
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
