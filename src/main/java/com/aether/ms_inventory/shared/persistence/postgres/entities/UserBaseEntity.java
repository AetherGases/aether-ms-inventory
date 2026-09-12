package com.aether.ms_inventory.shared.persistence.postgres.entities;

import com.aether.ms_inventory.shared.AetherConstants;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class UserBaseEntity extends DateBaseEntity{
  @Column(length = AetherConstants.MAX_EMAIL_LENGTH, nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", length = AetherConstants.MAX_PASSWORD_HASH_LENGTH)
  private String passwordHash;
}
