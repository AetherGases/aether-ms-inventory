package com.aether.ms_inventory.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends CoreApiException {
  public ConflictException(String message) {
    super(message);
  }

  public ConflictException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }
}
