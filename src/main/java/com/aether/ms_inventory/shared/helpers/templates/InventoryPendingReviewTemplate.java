package com.aether.ms_inventory.shared.helpers.templates;

import com.aether.ms_inventory.shared.helpers.interfaces.BrevoTemplate;

import java.util.Map;

public class InventoryPendingReviewTemplate implements BrevoTemplate {
  private final String approverName;
  private final String inventoryName;
  private final String ownerName;

  public InventoryPendingReviewTemplate(
      String approverName,
      String inventoryName,
      String ownerName
  ) {
    this.approverName = approverName;
    this.inventoryName = inventoryName;
    this.ownerName = ownerName;
  }

  @Override
  public int template() {
    return 2;
  }

  @Override
  public Map<String, Object> params() {
    return Map.of(
        "approverName", approverName,
        "inventoryName", inventoryName,
        "ownerName", ownerName
    );
  }
}
