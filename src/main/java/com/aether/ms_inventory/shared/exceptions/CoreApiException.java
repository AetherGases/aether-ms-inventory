package com.aether.ms_inventory.shared.exceptions;

import org.springframework.http.HttpStatus;

public abstract class CoreApiException extends RuntimeException{
  public CoreApiException(String message) {
    super(message);
  }

  public CoreApiException(String message, Throwable cause) {
    super(message, cause);
  }

  public abstract HttpStatus getStatus();
}