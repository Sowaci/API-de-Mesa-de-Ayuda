package com.helpdesk.api.dto.response;

import com.helpdesk.api.entity.TicketHistorial;
import com.helpdesk.api.enums.Estado;

import java.time.LocalDateTime;

public record TicketHistorialResponse(
        Long id,
        Long ticketId,
        Estado estadoAnterior,
        Estado estadoNuevo,
        LocalDateTime fecha,
        String usuarioEmail,
        String usuarioNombre
) {

    public static TicketHistorialResponse desde(TicketHistorial h) {
        return new TicketHistorialResponse(
                h.getId(),
                h.getTicket().getId(),
                h.getEstadoAnterior(),
                h.getEstadoNuevo(),
                h.getFecha(),
                h.getUsuario().getEmail(),
                h.getUsuario().getNombre()
        );
    }
}
