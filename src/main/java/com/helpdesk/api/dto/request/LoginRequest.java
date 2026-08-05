package com.helpdesk.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email debe tener un formato valido.")
        String email,

        @NotBlank(message = "La password es obligatoria.")
        String password
) {
}
