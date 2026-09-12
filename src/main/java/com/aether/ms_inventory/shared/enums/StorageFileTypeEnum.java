package com.aether.ms_inventory.shared.enums;

import lombok.Getter;

@Getter
public enum StorageFileTypeEnum {

  IMAGE(
      "images",
      "image"
  ),

  XLSX(
      "spreadsheets",
      "raw"
  );

  private final String cloudinaryFolder;
  private final String cloudinaryResourceType;

  StorageFileTypeEnum(
      String cloudinaryFolder,
      String cloudinaryResourceType
  ) {
    this.cloudinaryFolder = cloudinaryFolder;
    this.cloudinaryResourceType = cloudinaryResourceType;
  }
}
