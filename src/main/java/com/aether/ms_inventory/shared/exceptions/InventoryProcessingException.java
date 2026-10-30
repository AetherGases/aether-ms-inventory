package com.aether.ms_inventory.shared.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@Setter
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InventoryProcessingException extends MsInventoryException {
  private final boolean retryable;
  private final HttpStatus status;

  public InventoryProcessingException(String message, boolean retryable) {
    super(message);
    this.retryable = retryable;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public InventoryProcessingException(String message, boolean retryable, HttpStatus status) {
    super(message);
    this.retryable = retryable;
    this.status = status;
  }

  public InventoryProcessingException(String message, Throwable cause, boolean retryable) {
    super(message, cause);
    this.retryable = retryable;
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public InventoryProcessingException(String message, Throwable cause, boolean retryable, HttpStatus status) {
    super(message, cause);
    this.retryable = retryable;
    this.status = status;
  }
}
