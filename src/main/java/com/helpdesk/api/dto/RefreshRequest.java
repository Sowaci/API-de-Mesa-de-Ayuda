package com.helpdesk.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "El refreshToken es obligatorio")
        String refreshToken
) {
}
