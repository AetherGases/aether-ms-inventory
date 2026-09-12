package com.aether.ms_inventory.shared.persistence.postgres.repositories;

import com.aether.ms_inventory.shared.persistence.postgres.entities.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<PlanEntity, Integer> {
}
