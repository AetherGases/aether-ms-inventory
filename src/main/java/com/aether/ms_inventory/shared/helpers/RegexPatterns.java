package com.aether.ms_inventory.shared.helpers;

public class RegexPatterns {
  public static final String CPF = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$";
  public static final String NAME = "^[A-Za-zÀ-ÖØ-öø-ÿ]+(?:\\s+[A-Za-zÀ-ÖØ-öø-ÿ]+)*$";
  public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$";
  public static final String CEP = "^\\d{5}-?\\d{3}$";
  public static final String CODE = "^\\d{6}$";

  public static final String PHONE = "^(\\(\\d{2}\\)|\\d{2})\\s?\\d{4,5}-?\\d{4}$";
}
