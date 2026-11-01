package com.aether.ms_inventory.shared.security.jwt;

import com.aether.ms_inventory.shared.exceptions.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
  private final JwtTokenProvider tokenProvider;

  private final HandlerExceptionResolver handlerExceptionResolver;

  @Override
  public void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filter
  ) throws IOException, ServletException {

    try {
      String token = tokenProvider.resolveToken(request);

      if (StringUtils.isBlank(token)) {
        filter.doFilter(request, response);
        return;
      }

      if (!tokenProvider.validateToken(token)) {
        throw new UnauthorizedException("exception.jwt.required");
      }

      Authentication authentication =
          tokenProvider.getAuthentication(token);

      if (authentication == null) {
        throw new UnauthorizedException("exception.access.unhautorized");
      }

      SecurityContextHolder
          .getContext()
          .setAuthentication(authentication);

      filter.doFilter(request, response);

    } catch (UnauthorizedException e) {
      handlerExceptionResolver.resolveException(
          request,
          response,
          null,
          e
      );
    }
  }
}
