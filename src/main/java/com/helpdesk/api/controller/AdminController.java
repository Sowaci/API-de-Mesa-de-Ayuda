package com.helpdesk.api.controller;

import com.helpdesk.api.dto.request.AdminPromocionRequest;
import com.helpdesk.api.dto.response.EstadisticasResponse;
import com.helpdesk.api.entity.Usuario;
import com.helpdesk.api.enums.Estado;
import com.helpdesk.api.enums.Rol;
import com.helpdesk.api.exception.RecursoNoEncontradoException;
import com.helpdesk.api.repository.TicketRepository;
import com.helpdesk.api.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final TicketRepository ticketRepository;

    public AdminController(UsuarioRepository usuarioRepository, TicketRepository ticketRepository) {
        this.usuarioRepository = usuarioRepository;
        this.ticketRepository = ticketRepository;
    }

    @PostMapping("/soporte")
    @Transactional
    public ResponseEntity<Map<String, Object>> ascenderASoporte(@Valid @RequestBody AdminPromocionRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario con email " + request.email() + " no encontrado"));

        if (usuario.getRol() == Rol.ADMIN) {
            throw new IllegalArgumentException("El rol ADMIN no puede degradarse a SOPORTE");
        }

        usuario.setRol(Rol.SOPORTE);
        usuarioRepository.save(usuario);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("message", "Usuario ascendido al rol SOPORTE");
        respuesta.put("email", usuario.getEmail());
        respuesta.put("rol", usuario.getRol().name());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/estadisticas")
    public EstadisticasResponse estadisticas() {
        long resueltosDentro = ticketRepository.countResueltosDentroDeSla();
        long resueltosFuera = ticketRepository.countResueltosFueraDeSla();
        long totalResueltos = resueltosDentro + resueltosFuera;

        return new EstadisticasResponse(
                Map.of("ABIERTO", ticketRepository.countByEstado(Estado.ABIERTO),
                        "EN_PROCESO", ticketRepository.countByEstado(Estado.EN_PROCESO),
                        "RESUELTO", ticketRepository.countByEstado(Estado.RESUELTO)),
                ticketRepository.count(),
                resueltosDentro,
                resueltosFuera,
                totalResueltos,
                totalResueltos == 0 ? 0.0 : Math.round((resueltosDentro * 10000.0) / totalResueltos) / 100.0,
                ticketRepository.countVencidos(LocalDateTime.now())
        );
    }
}
