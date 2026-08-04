package com.helpdesk.api.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String nombre,
        String rol
) {
}
