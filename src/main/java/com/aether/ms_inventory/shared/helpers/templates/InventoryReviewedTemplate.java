package com.aether.ms_inventory.shared.helpers.templates;

import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.helpers.interfaces.BrevoTemplate;

import java.util.Map;

public class InventoryReviewedTemplate implements BrevoTemplate {
  private final String approverName;
  private final String validationMessage;
  private final String inventoryName;
  private final String ownerName;
  private final InventoryStatusEnum inventoryStatus;

  public InventoryReviewedTemplate(
      String approverName,
      String validationMessage,
      String inventoryName,
      String ownerName,
      InventoryStatusEnum inventoryStatus
  ) {
    this.approverName = approverName;
    this.validationMessage = validationMessage;
    this.inventoryName = inventoryName;
    this.ownerName = ownerName;
    this.inventoryStatus = inventoryStatus;
  }

  @Override
  public int template() {
    return 3;
  }

  @Override
  public Map<String, Object> params() {
    return Map.of(
        "approverName", approverName,
        "validationMessage", validationMessage,
        "inventoryName", inventoryName,
        "ownerName", ownerName,
        "inventoryStatus", inventoryStatus == InventoryStatusEnum.APPROVED ? "aprovado" : "reprovado"
    );
  }
}
