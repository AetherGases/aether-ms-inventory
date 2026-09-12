package com.aether.ms_inventory.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends CoreApiException {
  public BadRequestException(String message) {
    super(message);
  }

  public BadRequestException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.BAD_REQUEST;
  }
}
