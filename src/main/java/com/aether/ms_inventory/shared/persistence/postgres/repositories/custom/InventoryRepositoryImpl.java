package com.aether.ms_inventory.shared.persistence.postgres.repositories.custom;

import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.custom.interfaces.InventoryRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepositoryCustom {

  @PersistenceContext
  private final EntityManager entityManager;

  @Override
  public List<InventoryEntity> findWithPagination(
      Specification<InventoryEntity> specification,
      Integer skip,
      Integer take
  ) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    CriteriaQuery<InventoryEntity> query =
        cb.createQuery(InventoryEntity.class);

    Root<InventoryEntity> root =
        query.from(InventoryEntity.class);

    Predicate predicate =
        specification.toPredicate(root, query, cb);

    query.where(predicate);

    query.orderBy(cb.desc(root.get("createdAt")));

    return entityManager
        .createQuery(query)
        .setFirstResult(skip)
        .setMaxResults(take)
        .getResultList();
  }
}
