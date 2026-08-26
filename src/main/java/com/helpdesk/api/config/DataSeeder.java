package com.helpdesk.api.config;

import com.helpdesk.api.entity.Ticket;
import com.helpdesk.api.entity.Usuario;
import com.helpdesk.api.enums.Estado;
import com.helpdesk.api.enums.Prioridad;
import com.helpdesk.api.enums.Rol;
import com.helpdesk.api.repository.TicketRepository;
import com.helpdesk.api.repository.UsuarioRepository;
import com.helpdesk.api.service.SlaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

        private final UsuarioRepository usuarioRepository;
        private final TicketRepository ticketRepository;
        private final PasswordEncoder passwordEncoder;
        private final SlaService slaService;

        public DataSeeder(UsuarioRepository usuarioRepository, TicketRepository ticketRepository,
                        PasswordEncoder passwordEncoder, SlaService slaService) {
                this.usuarioRepository = usuarioRepository;
                this.ticketRepository = ticketRepository;
                this.passwordEncoder = passwordEncoder;
                this.slaService = slaService;
        }

        @Override
        @Transactional
        public void run(String... args) {
                if (usuarioRepository.count() > 0) {
                        return;
                }

                Usuario admin = usuarioRepository.save(Usuario.builder()
                                .nombre("Administrador")
                                .email("admin@helpdesk.com")
                                .password(passwordEncoder.encode("admin123"))
                                .rol(Rol.ADMIN)
                                .build());

                Usuario soporte = usuarioRepository.save(Usuario.builder()
                                .nombre("Soporte Tecnico")
                                .email("soporte@helpdesk.com")
                                .password(passwordEncoder.encode("soporte123"))
                                .rol(Rol.SOPORTE)
                                .build());

                Usuario usuario = usuarioRepository.save(Usuario.builder()
                                .nombre("Usuario Prueba")
                                .email("usuario@helpdesk.com")
                                .password(passwordEncoder.encode("usuario123"))
                                .rol(Rol.USUARIO)
                                .build());

                LocalDateTime ahora = LocalDateTime.now();

                // TICKETS CREADOS POR ADMIN (rol ADMIN)
                LocalDateTime adminVencido = ahora.minusDays(3);
                ticketRepository.save(Ticket.builder()
                                .titulo("Caida de base de datos crítica.")
                                .descripcion("La base de datos principal no responde transacciones.")
                                .prioridad(Prioridad.ALTA)
                                .estado(Estado.ABIERTO)
                                .creadoEn(adminVencido)
                                .slaVenceEn(slaService.calcularVencimientoSla(adminVencido, Prioridad.ALTA))
                                .creadoPor(admin)
                                .build());

                LocalDateTime adminActivo = ahora.minusHours(1);
                ticketRepository.save(Ticket.builder()
                                .titulo("Nuevo usuario no puede registrarse.")
                                .descripcion("El formulario de registro da error de validación.")
                                .prioridad(Prioridad.MEDIA)
                                .estado(Estado.ABIERTO)
                                .creadoEn(adminActivo)
                                .slaVenceEn(slaService.calcularVencimientoSla(adminActivo, Prioridad.MEDIA))
                                .creadoPor(admin)
                                .build());

                // TICKETS CREADOS POR SOPORTE (rol SOPORTE)
                LocalDateTime soporteVencido = ahora.minusDays(5);
                ticketRepository.save(Ticket.builder()
                                .titulo("Impresora de la sala 4 no imprime.")
                                .descripcion("Error de papel atascado, ya se revisó hardware.")
                                .prioridad(Prioridad.BAJA)
                                .estado(Estado.ABIERTO)
                                .creadoEn(soporteVencido)
                                .slaVenceEn(slaService.calcularVencimientoSla(soporteVencido, Prioridad.BAJA))
                                .creadoPor(soporte)
                                .build());

                LocalDateTime soporteProceso = ahora.minusHours(5);
                ticketRepository.save(Ticket.builder()
                                .titulo("Acceso VPN lento para usuario nuevo.")
                                .descripcion("La conexión VPN establece pero la velocidad es insuficiente.")
                                .prioridad(Prioridad.MEDIA)
                                .estado(Estado.EN_PROCESO)
                                .creadoEn(soporteProceso)
                                .slaVenceEn(slaService.calcularVencimientoSla(soporteProceso, Prioridad.MEDIA))
                                .creadoPor(soporte)
                                .build());

                // TICKETS CREADOS POR USUARIO (rol USUARIO)
                LocalDateTime usuarioVencido = ahora.minusDays(2);
                ticketRepository.save(Ticket.builder()
                                .titulo("No llega email de bienvenida.")
                                .descripcion("El usuario no recibe el email después de registrarse.")
                                .prioridad(Prioridad.ALTA)
                                .estado(Estado.ABIERTO)
                                .creadoEn(usuarioVencido)
                                .slaVenceEn(slaService.calcularVencimientoSla(usuarioVencido, Prioridad.ALTA))
                                .creadoPor(usuario)
                                .build());

                LocalDateTime usuarioResuelto = ahora.minusHours(10);
                ticketRepository.save(Ticket.builder()
                                .titulo("Duda sobre uso del panel de control.")
                                .descripcion("El usuario no encuentra la opción de editar perfil.")
                                .prioridad(Prioridad.BAJA)
                                .estado(Estado.RESUELTO)
                                .creadoEn(usuarioResuelto)
                                .slaVenceEn(slaService.calcularVencimientoSla(usuarioResuelto, Prioridad.BAJA))
                                .resueltoEn(usuarioResuelto.plusHours(2))
                                .creadoPor(usuario)
                                .build());
        }
}
