package com.aether.ms_inventory.shared.handlers.dto.output;

import org.springframework.http.HttpStatus;

public record ExceptionOutputDTO(
    String message,
    HttpStatus status
) {
}
