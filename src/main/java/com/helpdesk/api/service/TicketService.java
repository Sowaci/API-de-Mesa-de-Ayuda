package com.helpdesk.api.service;

import com.helpdesk.api.dto.response.TicketHistorialResponse;
import com.helpdesk.api.dto.request.TicketRequest;
import com.helpdesk.api.dto.response.TicketResponse;
import com.helpdesk.api.entity.Ticket;
import com.helpdesk.api.entity.TicketHistorial;
import com.helpdesk.api.entity.Usuario;
import com.helpdesk.api.enums.Estado;
import com.helpdesk.api.enums.Rol;
import com.helpdesk.api.exception.RecursoNoEncontradoException;
import com.helpdesk.api.repository.TicketHistorialRepository;
import com.helpdesk.api.repository.TicketRepository;
import com.helpdesk.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final TicketHistorialRepository historialRepository;
    private final SlaService slaService;

    public TicketService(TicketRepository ticketRepository,
            UsuarioRepository usuarioRepository,
            TicketHistorialRepository historialRepository,
            SlaService slaService) {
        this.ticketRepository = ticketRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialRepository = historialRepository;
        this.slaService = slaService;
    }

    @Transactional
    public TicketResponse crear(TicketRequest request, Long creadorId) {
        Usuario creador = usuarioRepository.findById(creadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        LocalDateTime ahora = LocalDateTime.now();
        Ticket ticket = Ticket.builder()
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .prioridad(request.prioridad())
                .estado(Estado.ABIERTO)
                .creadoEn(ahora)

                .slaVenceEn(slaService.calcularVencimientoSla(ahora, request.prioridad()))
                .creadoPor(creador)
                .build();

        return TicketResponse.desde(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> misTickets(Long usuarioId) {
        return ticketRepository.findByCreadoPorId(usuarioId)
                .stream().map(TicketResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse porId(Long ticketId, Usuario usuario) {
        Ticket ticket = obtener(ticketId);
        boolean esDueno = ticket.getCreadoPor().getId().equals(usuario.getId());
        boolean esStaff = usuario.getRol() == Rol.SOPORTE || usuario.getRol() == Rol.ADMIN;
        if (!esDueno && !esStaff) {
            throw new AccessDeniedException("Un usuario solo puede consultar sus propios tickets.");
        }
        return TicketResponse.desde(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> todos(Pageable pageable) {
        return ticketRepository.findAll(pageable).map(TicketResponse::desde);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> vencidos() {
        return ticketRepository
                .findByEstadoNotAndSlaVenceEnBefore(Estado.RESUELTO, LocalDateTime.now())
                .stream().map(TicketResponse::desde).toList();
    }

    @Transactional
    public TicketResponse cambiarEstado(Long ticketId, Estado nuevoEstado, Usuario usuario) {
        Ticket ticket = obtener(ticketId);

        Estado anterior = ticket.getEstado();
        if (anterior != nuevoEstado) {
            ticket.setEstado(nuevoEstado);

            if (nuevoEstado == Estado.RESUELTO) {
                ticket.setResueltoEn(LocalDateTime.now());
            } else if (anterior == Estado.RESUELTO) {
                ticket.setResueltoEn(null);
            }

            historialRepository.save(TicketHistorial.builder()
                    .ticket(ticket)
                    .estadoAnterior(anterior)
                    .estadoNuevo(nuevoEstado)
                    .usuario(usuario)
                    .fecha(LocalDateTime.now())
                    .build());
        }

        return TicketResponse.desde(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketHistorialResponse> historial(Long ticketId, Usuario usuario) {

        porId(ticketId, usuario);
        return historialRepository.findByTicketIdOrderByFechaAsc(ticketId)
                .stream().map(TicketHistorialResponse::desde).toList();
    }

    private Ticket obtener(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket " + ticketId + " no encontrado"));
    }
}
