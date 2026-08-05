package com.helpdesk.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        String nombre,

        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email debe tener un formato valido.")
        String email,

        @NotBlank(message = "La password es obligatoria")
        @Size(min = 6, message = "La password debe tener al menos 6 caracteres.")
        String password
) {
}
