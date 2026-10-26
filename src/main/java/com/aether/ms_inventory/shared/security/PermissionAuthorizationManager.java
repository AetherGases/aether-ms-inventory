package com.aether.ms_inventory.shared.security;

import com.aether.ms_inventory.shared.helpers.CustomUserDetails;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
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
  private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

  @Override
  public AuthorizationResult authorize(
      Supplier<? extends Authentication> authentication,
      RequestAuthorizationContext context
  ) {
    Authentication auth = authentication.get();

    if (!trustResolver.isAuthenticated(auth)
        || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
      return new AuthorizationDecision(false);
    }

    String requestUrl = context.getRequest().getRequestURI();

    return new AuthorizationDecision(permissionMatches(user.getId(), requestUrl));
  }

  private boolean permissionMatches(
      Integer employeeId,
      String requestUrl
  ) {
    return permissionRepository.findAllByEmployeeId(employeeId)
        .stream()
        .anyMatch(permission -> requestUrl.matches(permission.getPattern()));
  }
}
