package com.aether.ms_inventory.shared.helpers;

public class NormalizeInput {
  public static String cpf(String cpf){
    if (cpf == null) return cpf;

    return removeBlank(cpf).replaceAll("\\D", "");
  }

  public static String name(String name){
    if (name == null) return name;

    return removeBlank(name).toLowerCase();
  }

  public static String email(String email){
    if (email == null) return email;

    return removeBlank(email).toLowerCase();
  }

  public static String password(String password){
    if (password == null) return password;

    return removeBlank(password);
  }

  public static String phone(String phone) {
    if (phone == null) return phone;

    return removeBlank(phone).replaceAll("\\D", "");
  }

  private static String removeBlank(String input){
    return input.trim();
  }
}
