package com.helpdesk.api.dto.response;

import com.helpdesk.api.entity.Ticket;
import com.helpdesk.api.enums.Estado;
import com.helpdesk.api.enums.Prioridad;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String titulo,
        String descripcion,
        Prioridad prioridad,
        Estado estado,
        LocalDateTime creadoEn,
        LocalDateTime slaVenceEn,
        boolean vencido,
        String creadoPorEmail,
        String creadoPorNombre
) {

    public static TicketResponse desde(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitulo(),
                ticket.getDescripcion(),
                ticket.getPrioridad(),
                ticket.getEstado(),
                ticket.getCreadoEn(),
                ticket.getSlaVenceEn(),
                ticket.estaVencido(),
                ticket.getCreadoPor().getEmail(),
                ticket.getCreadoPor().getNombre()
        );
    }
}
