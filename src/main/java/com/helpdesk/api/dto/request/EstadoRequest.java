package com.helpdesk.api.dto.request;

import com.helpdesk.api.enums.Estado;
import jakarta.validation.constraints.NotNull;

public record EstadoRequest(
        @NotNull(message = "El estado es obligatorio (ABIERTO, EN_PROCESO o RESUELTO)")
        Estado estado
) {
}
