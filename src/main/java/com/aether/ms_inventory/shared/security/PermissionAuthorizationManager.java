package com.aether.ms_inventory.shared.security;

import com.aether.ms_inventory.shared.persistence.postgres.entities.PermissionEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class PermissionAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {
  private final PermissionRepository permissionRepository;

  @Override
  public AuthorizationResult authorize(
      Supplier<? extends Authentication> authentication,
      RequestAuthorizationContext context
  ) {

    Authentication auth = authentication.get();

    if (auth == null || !auth.isAuthenticated()) {
      return new AuthorizationDecision(false);
    }

    String requestUrl = context.getRequest().getRequestURI();

    boolean hasPermission = auth.getAuthorities()
        .stream()
        .anyMatch(authority ->
            permissionMatches(
                authority.getAuthority(),
                requestUrl
            )
        );

    return new AuthorizationDecision(hasPermission);
  }

  private boolean permissionMatches(
      String permissionName,
      String requestUrl
  ) {
    PermissionEntity permission = permissionRepository.findByNameIgnoreCase(permissionName);

    if (permission != null && requestUrl.matches(permission.getUrl())) return true;

    return false;
  }
}