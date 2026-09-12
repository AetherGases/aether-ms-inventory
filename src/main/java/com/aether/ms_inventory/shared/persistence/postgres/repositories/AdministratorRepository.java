package com.aether.ms_inventory.shared.persistence.postgres.repositories;

import com.aether.ms_inventory.shared.persistence.postgres.entities.AdministratorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdministratorRepository extends JpaRepository<AdministratorEntity, Integer> {
}
