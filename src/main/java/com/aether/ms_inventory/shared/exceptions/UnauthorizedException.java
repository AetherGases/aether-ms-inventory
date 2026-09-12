package com.aether.ms_inventory.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends CoreApiException {
  public UnauthorizedException(String message) {
    super(message);
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.UNAUTHORIZED;
  }
}
