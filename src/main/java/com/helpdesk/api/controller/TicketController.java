package com.helpdesk.api.controller;

import com.helpdesk.api.dto.EstadoRequest;
import com.helpdesk.api.dto.TicketRequest;
import com.helpdesk.api.dto.TicketResponse;
import com.helpdesk.api.entity.Usuario;
import com.helpdesk.api.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> crear(@Valid @RequestBody TicketRequest request,
                                                @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.crear(request, usuario.getId()));
    }

    @GetMapping("/mios")
    public List<TicketResponse> mios(@AuthenticationPrincipal Usuario usuario) {
        return ticketService.misTickets(usuario.getId());
    }

    @GetMapping("/vencidos")
    public List<TicketResponse> vencidos() {
        return ticketService.vencidos();
    }

    @GetMapping
    public Page<TicketResponse> todos(@PageableDefault(size = 10, sort = "creadoEn") Pageable pageable) {
        return ticketService.todos(pageable);
    }

    @GetMapping("/{id}")
    public TicketResponse porId(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ticketService.porId(id, usuario);
    }

    @PatchMapping("/{id}/estado")
    public TicketResponse cambiarEstado(@PathVariable Long id,
                                        @Valid @RequestBody EstadoRequest request,
                                        @AuthenticationPrincipal Usuario usuario) {
        return ticketService.cambiarEstado(id, request.estado(), usuario);
    }
}
