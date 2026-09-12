package com.aether.ms_inventory.shared.helpers;


import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class CustomUserDetails extends User {

  private final Integer id;

  public CustomUserDetails(
      String username,
      @Nullable String password,
      List<SimpleGrantedAuthority> authorities,
      Integer id
  ) {
    super(username, password, authorities);
    this.id = id;
  }
}