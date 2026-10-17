package com.aether.ms_inventory.shared.persistence.postgres.specifications;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.aether.ms_inventory.shared.persistence.postgres.entities.InventoryEntity;
import org.springframework.data.jpa.domain.Specification;

public class InventorySpecification {
  public static Specification<InventoryEntity> withName(String name) {
    return (root, query, cb) -> name == null ? null
        : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
  }

  public static Specification<InventoryEntity> withDepartment(Integer departmentId){
    return (root, query, cb) -> departmentId == null ? null
        : cb.equal(root.get("department").get("id"), departmentId);
  }

  public static Specification<InventoryEntity> withStatus(InventoryStatusEnum status){
    return (root, query, cb) -> status == null ? null
        : cb.equal(root.get("status"), status);
  }

  public static Specification<InventoryEntity> withType(InventoryTypeEnum type){
    return (root, query, cb) -> type == null ? null
        : cb.equal(root.get("type"), type);
  }

  public static Specification<InventoryEntity> withNotProcessing() {
    return (root, query, cb) ->
        root.get("status").in(
            InventoryStatusEnum.PROCESSING_FAILED,
            InventoryStatusEnum.PROCESSING,
            InventoryStatusEnum.REQUESTED
        ).not();
  }

  public static Specification<InventoryEntity> withOwnerEmployeeId(Integer ownerEmployeeId) {
    return (root, query, cb) -> {
      if (ownerEmployeeId == null) {
        return null;
      }

      return cb.equal(
          root.get("ownerEmployee").get("id"),
          ownerEmployeeId
      );
    };
  }

  public static Specification<InventoryEntity> withNotOwnerEmployeeId(Integer ownerEmployeeId) {
    return (root, query, cb) -> {
      if (ownerEmployeeId == null) {
        return null;
      }

      return cb.notEqual(
          root.get("ownerEmployee").get("id"),
          ownerEmployeeId
      );
    };
  }

  public static Specification<InventoryEntity> withValidatorEmployeeId(Integer validatorEmployeeId) {
    return (root, query, cb) -> {
      if (validatorEmployeeId == null) {
        return null;
      }

      return cb.equal(
          root.get("validatorEmployee").get("id"),
          validatorEmployeeId
      );
    };
  }
}
