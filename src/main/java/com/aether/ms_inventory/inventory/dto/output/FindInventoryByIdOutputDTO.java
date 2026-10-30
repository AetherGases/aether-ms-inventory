package com.aether.ms_inventory.inventory.dto.output;

import com.aether.ms_inventory.shared.enums.CategoryClassificationEnum;
import com.aether.ms_inventory.shared.enums.InventoryStatusEnum;
import com.aether.ms_inventory.shared.enums.InventoryTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FindInventoryByIdOutputDTO(
    Integer id,
    String name,
    String description,
    String consolidationApproach,
    LocalDate inventoryPeriodStart,
    LocalDate inventoryPeriodEnd,
    InventoryTypeEnum type,
    InventoryStatusEnum status,
    String validationMessage,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Employee owner,
    Employee validator,
    StorageFile storageFile,
    List<Emission> emissions,
    List<Reduction> reductions
) {
  public record Employee(
      Integer id,
      String name,
      String cpf,
      String imageUrl
  ){}

  public record StorageFile(
      Integer id,
      String fileName,
      String path,
      LocalDateTime createdAt
  ){}

  public record Emission(
      Integer id,
      BigDecimal quantityCo2e,
      String methodologyDescription,
      BigDecimal supplierDataPercentage,
      Scope scope,
      Category category,
      Gas gas,
      @JsonProperty("isReduction")
      boolean isReduction,
      LocalDateTime createdAt
  ){}

  public record Reduction(
      Integer id,
      BigDecimal quantityCo2e,
      Category category,
      @JsonProperty("isReduction")
      boolean isReduction,
      LocalDateTime createdAt
  ){}

  public record Scope(
      Integer id,
      String name
  ){}

  public record Category(
      Integer id,
      String name,
      CategoryClassificationEnum classification
  ){}

  public record Gas(
      Integer id,
      String name,
      String formula,
      Boolean biogenic,
      BigDecimal gwp
  ){}
}
