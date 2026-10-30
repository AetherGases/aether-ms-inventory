package com.aether.ms_inventory.shared.dto.output;

import org.springframework.http.HttpStatus;

public record ExceptionOutputDTO(
    String message,
    HttpStatus status
) {
}
