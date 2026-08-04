package com.helpdesk.api.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String nombre,
        String rol
) {
}
