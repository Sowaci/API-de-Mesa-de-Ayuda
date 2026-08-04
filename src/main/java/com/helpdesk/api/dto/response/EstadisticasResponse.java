package com.helpdesk.api.dto.response;

import java.util.Map;

public record EstadisticasResponse(
        Map<String, Long> ticketsPorEstado,
        long totalTickets,
        long resueltosDentroDeSla,
        long resueltosFueraDeSla,
        long totalResueltos,
        double porcentajeCumplimientoSla,
        long ticketsVencidos
) {
}
