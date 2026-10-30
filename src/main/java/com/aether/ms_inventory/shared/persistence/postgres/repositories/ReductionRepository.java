package com.aether.ms_inventory.shared.persistence.postgres.repositories;

import com.aether.ms_inventory.shared.persistence.postgres.entities.ReductionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ReductionRepository extends JpaRepository<ReductionEntity, Integer> {

  @Query(
      value = """
          CALL sp_register_reduction(
              CAST(NULL AS integer),
              CAST(:quantityCo2e AS numeric),
              CAST(:idInventory AS integer),
              CAST(:idCategory AS integer),
              CAST(:appActorId AS text),
              CAST(:logicalOrigin AS text)
          )
          """,
      nativeQuery = true
  )
  Integer registerReduction(
      @Param("quantityCo2e") BigDecimal quantityCo2e,
      @Param("idInventory") Integer idInventory,
      @Param("idCategory") Integer idCategory,
      @Param("appActorId") String appActorId,
      @Param("logicalOrigin") String logicalOrigin
  );
}
