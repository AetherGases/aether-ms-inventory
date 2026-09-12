package com.aether.ms_inventory.shared.security.dto.output;

import java.util.Date;

public record LoginOutputDTO(
    String email,
    Boolean authenticated,
    Date created,
    Date expiration,
    String accessToken,
    String refreshToken
) {
}
