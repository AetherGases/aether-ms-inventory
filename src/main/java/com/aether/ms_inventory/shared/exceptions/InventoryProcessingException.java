package com.aether.ms_inventory.shared.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InventoryProcessingException extends MsInventoryException {
  private final boolean retryable;

  public InventoryProcessingException(String message, boolean retryable) {
    super(message);
    this.retryable = retryable;
  }

  public InventoryProcessingException(String message, Throwable cause, boolean retryable) {
    super(message, cause);
    this.retryable = retryable;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
