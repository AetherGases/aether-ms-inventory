package com.aether.ms_inventory.shared;

public final class AetherConstants {

  private AetherConstants() {}

  public static final int MIN_AGE = 18;

  public static final int KEY_LENGTH = 36;

  // Common
  public static final int ZIP_CODE_LENGTH = 8;
  public static final int CNPJ_LENGTH = 14;
  public static final int CPF_LENGTH = 11;
  public static final int CNAE_LENGTH = 7;
  public static final int MAX_EMAIL_LENGTH = 255;
  public static final int MIN_EMAIL_LENGTH = 5;
  public static final int MAX_PASSWORD_LENGTH = 28;
  public static final int MIN_PASSWORD_LENGTH = 8;
  public static final int MAX_PASSWORD_HASH_LENGTH = 255;

  // Plan

  public static final int MAX_PLAN_NAME_LENGTH = 50;
  public static final int MIN_PLAN_NAME_LENGTH = 3;

  public static final int MAX_PLAN_DESCRIPTION_LENGTH = 150;
  public static final int MIN_PLAN_DESCRIPTION_LENGTH = 3;

  // Address
  public static final int MAX_STATE_LENGTH = 150;
  public static final int MIN_STATE_LENGTH = 3;
  public static final int MAX_NEIGHBORHOOD_LENGTH = 150;
  public static final int MIN_NEIGHBORHOOD_LENGTH = 3;
  public static final int MAX_CITY_LENGTH = 150;
  public static final int MIN_CITY_LENGTH = 3;

  public static final int MAX_ADDRESS_COMPLEMENT_LENGTH = 150;
  public static final int MIN_ADDRESS_COMPLEMENT_LENGTH = 3;

  public static final int MAX_ADDRESS_STREET_LENGTH = 255;
  public static final int MIN_ADDRESS_STREET_LENGTH = 3;

  // Enterprise

  public static final int MAX_ENTERPRISE_NAME_LENGTH = 150;
  public static final int MIN_ENTERPRISE_NAME_LENGTH = 3;

  public static final int MAX_ENTERPRISE_TRADE_NAME_LENGTH = 150;
  public static final int MIN_ENTERPRISE_TRADE_NAME_LENGTH = 3;

  // Department

  public static final int MAX_DEPARTMENT_NAME_LENGTH = 150;
  public static final int MIN_DEPARTMENT_NAME_LENGTH = 3;

  public static final int MAX_DEPARTMENT_DESCRIPTION_LENGTH = 150;
  public static final int MIN_DEPARTMENT_DESCRIPTION_LENGTH = 3;

  // Permission Group

  public static final int MAX_PERMISSION_GROUP_DESCRIPTION_LENGTH = 150;
  public static final int MIN_PERMISSION_GROUP_DESCRIPTION_LENGTH = 3;

  // Permission

  public static final int MAX_PERMISSION_NAME_LENGTH = 150;
  public static final int MIN_PERMISSION_NAME_LENGTH = 3;

  public static final int MAX_PERMISSION_DESCRIPTION_LENGTH = 150;
  public static final int MIN_PERMISSION_DESCRIPTION_LENGTH = 3;

  // Storage File

  public static final int MAX_STORAGE_FILE_NAME_LENGTH = 150;
  public static final int MIN_STORAGE_FILE_NAME_LENGTH = 3;

  public static final int MAX_STORAGE_FILE_PATH_LENGTH = 255;
  public static final int MIN_STORAGE_FILE_PATH_LENGTH = 3;

  // Employee

  public static final int MAX_EMPLOYEE_NAME_LENGTH = 150;
  public static final int MIN_EMPLOYEE_NAME_LENGTH = 3;

  public static final int MAX_EMPLOYEE_PHONE_LENGTH = 20;
  public static final int MIN_EMPLOYEE_PHONE_LENGTH = 10;

  // Scope

  public static final int MAX_SCOPE_NAME_LENGTH = 100;
  public static final int MIN_SCOPE_NAME_LENGTH = 3;

  // Category

  public static final int MAX_CATEGORY_NAME_LENGTH = 150;
  public static final int MIN_CATEGORY_NAME_LENGTH = 3;

  // Gas

  public static final int MAX_GAS_NAME_LENGTH = 150;
  public static final int MIN_GAS_NAME_LENGTH = 2;

  public static final int MAX_GAS_FORMULA_LENGTH = 150;
  public static final int MIN_GAS_FORMULA_LENGTH = 1;

  // Inventory

  public static final int MAX_INVENTORY_NAME_LENGTH = 150;
  public static final int MIN_INVENTORY_NAME_LENGTH = 3;

  public static final int MAX_INVENTORY_DESCRIPTION_LENGTH = 150;
  public static final int MIN_INVENTORY_DESCRIPTION_LENGTH = 3;

  public static final int MAX_INVENTORY_CONSOLIDATION_APPROACH_LENGTH = 100;
  public static final int MIN_INVENTORY_CONSOLIDATION_APPROACH_LENGTH = 3;

  // Emission

  public static final int MAX_EMISSION_METHODOLOGY_DESCRIPTION_LENGTH = 150;
  public static final int MIN_EMISSION_METHODOLOGY_DESCRIPTION_LENGTH = 3;
}