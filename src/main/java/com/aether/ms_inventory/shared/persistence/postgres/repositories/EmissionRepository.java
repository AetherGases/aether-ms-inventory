package com.aether.ms_inventory.shared.persistence.postgres.repositories;

import com.aether.ms_inventory.shared.persistence.postgres.entities.EmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface EmissionRepository extends JpaRepository<EmissionEntity, Integer> {

  @Query(
      value = """
          CALL sp_register_emission(
              CAST(NULL AS integer),
              CAST(:quantityCo2e AS numeric),
              CAST(:idInventory AS integer),
              CAST(:idGas AS integer),
              CAST(:idScope AS integer),
              CAST(:idCategory AS integer),
              CAST(:methodologyDescription AS varchar),
              CAST(:supplierDataPercentage AS numeric),
              CAST(:appActorId AS text),
              CAST(:logicalOrigin AS text)
          )
          """,
      nativeQuery = true
  )
  Integer registerEmission(
      @Param("quantityCo2e") BigDecimal quantityCo2e,
      @Param("idInventory") Integer idInventory,
      @Param("idGas") Integer idGas,
      @Param("idScope") Integer idScope,
      @Param("idCategory") Integer idCategory,
      @Param("methodologyDescription") String methodologyDescription,
      @Param("supplierDataPercentage") BigDecimal supplierDataPercentage,
      @Param("appActorId") String appActorId,
      @Param("logicalOrigin") String logicalOrigin
  );
}
