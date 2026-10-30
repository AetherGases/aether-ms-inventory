package com.aether.ms_inventory.shared.exceptions;

import org.springframework.http.HttpStatus;

public abstract class MsInventoryException extends RuntimeException{
  public MsInventoryException(String message) {
    super(message);
  }

  public MsInventoryException(String message, Throwable cause) {
    super(message, cause);
  }

  public abstract HttpStatus getStatus();
}