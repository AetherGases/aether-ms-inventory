package com.aether.ms_inventory.shared.helpers;

import java.util.Set;

public class NormalizeOutput {
  public static String cpf(String cpf){
    if (cpf == null || cpf.isBlank()) return cpf;

    String digits = cpf.replaceAll("\\D", "");

    return String.format(
        "%s.%s.%s-%s",
        digits.substring(0, 3),
        digits.substring(3, 6),
        digits.substring(6, 9),
        digits.substring(9, 11)
    );
  }

  public static String email(String email){
    if (email == null || email.isBlank()) return email;

    return email.toLowerCase();
  }

  public static String name(String name){
    if (name == null || name.isBlank()) return name;

    String[] words = name.trim()
        .replaceAll("\\s+", " ")
        .toLowerCase()
        .split(" ");

    StringBuilder normalized = new StringBuilder();

    for (int i = 0; i < words.length; i++) {
      String word = words[i];

      if (i > 0 && LOWERCASE_WORDS.contains(word)) {
        normalized.append(word);
      } else {
        normalized.append(
            Character.toUpperCase(word.charAt(0))
        );

        if (word.length() > 1) {
          normalized.append(word.substring(1));
        }
      }

      if (i < words.length - 1) {
        normalized.append(' ');
      }
    }

    return normalized.toString();
  }

  public static String description(String description){
    if (description == null || description.isBlank()) return description;

    return description;
  }

  public static String url(String url){
    if (url == null || url.isBlank()) return url;

    return url;
  }

  public static String cep(String cep){
    if (cep == null || cep.isBlank()) return cep;

    return cep.substring(0, 5) + "-" + cep.substring(5);
  }

  public static String addressInfos(String info){
    if (info == null || info.isBlank()) return info;

    String[] words = info.trim()
        .replaceAll("\\s+", " ")
        .toLowerCase()
        .split(" ");

    StringBuilder normalized = new StringBuilder();

    for (int i = 0; i < words.length; i++) {
      String word = words[i];

      if (i > 0 && LOWERCASE_WORDS.contains(word)) {
        normalized.append(word);
      } else {
        normalized.append(
            Character.toUpperCase(word.charAt(0))
        );

        if (word.length() > 1) {
          normalized.append(word.substring(1));
        }
      }

      if (i < words.length - 1) {
        normalized.append(' ');
      }
    }

    return normalized.toString();
  }

  private static final Set<String> LOWERCASE_WORDS = Set.of(
      "de", "da", "do", "das", "dos", "e", "o", "a", "em", "na", "no"
  );
}
