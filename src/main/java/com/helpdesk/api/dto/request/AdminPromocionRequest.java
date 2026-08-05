package com.helpdesk.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminPromocionRequest(
        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email debe tener un formato valido.")
        String email
) {
}
