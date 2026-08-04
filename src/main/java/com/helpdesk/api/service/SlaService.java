package com.helpdesk.api.service;

import com.helpdesk.api.enums.Prioridad;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class SlaService {

    private final long horasAlta;
    private final long horasMedia;
    private final long horasBaja;

    public SlaService(@Value("${helpdesk.sla.alta:4}") long horasAlta,
                      @Value("${helpdesk.sla.media:24}") long horasMedia,
                      @Value("${helpdesk.sla.baja:72}") long horasBaja) {
        this.horasAlta = horasAlta;
        this.horasMedia = horasMedia;
        this.horasBaja = horasBaja;
    }

    public LocalDateTime calcularVencimientoSla(LocalDateTime creadoEn, Prioridad prioridad) {
        return creadoEn.plus(duracion(prioridad));
    }

    public Duration duracion(Prioridad prioridad) {
        return switch (prioridad) {
            case ALTA -> Duration.ofHours(horasAlta);
            case MEDIA -> Duration.ofHours(horasMedia);
            case BAJA -> Duration.ofHours(horasBaja);
        };
    }
}
