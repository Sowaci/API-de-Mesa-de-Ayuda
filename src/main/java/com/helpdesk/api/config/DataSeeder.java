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

                LocalDateTime creadoVencido = ahora.minusDays(10);
                ticketRepository.save(Ticket.builder()
                                .titulo("Servidor de produccion caido.")
                                .descripcion("El servidor principal no responde desde hace días.")
                                .prioridad(Prioridad.ALTA)
                                .estado(Estado.ABIERTO)
                                .creadoEn(creadoVencido)
                                .slaVenceEn(slaService.calcularVencimientoSla(creadoVencido, Prioridad.ALTA))
                                .creadoPor(usuario)
                                .build());

                LocalDateTime creadoProceso = ahora.minusHours(2);
                ticketRepository.save(Ticket.builder()
                                .titulo("No puedo iniciar sesion en la VPN.")
                                .descripcion("Error de autenticacion al conectar la VPN corporativa.")
                                .prioridad(Prioridad.MEDIA)
                                .estado(Estado.EN_PROCESO)
                                .creadoEn(creadoProceso)
                                .slaVenceEn(slaService.calcularVencimientoSla(creadoProceso, Prioridad.MEDIA))
                                .creadoPor(usuario)
                                .build());

                LocalDateTime creadoResuelto = ahora.minusDays(3);
                ticketRepository.save(Ticket.builder()
                                .titulo("Impresora de la oficina no imprime.")
                                .descripcion("La impresora muestra error de papel atascado.")
                                .prioridad(Prioridad.BAJA)
                                .estado(Estado.RESUELTO)
                                .creadoEn(creadoResuelto)
                                .slaVenceEn(slaService.calcularVencimientoSla(creadoResuelto, Prioridad.BAJA))
                                .resueltoEn(creadoResuelto.plusDays(1))
                                .creadoPor(usuario)
                                .build());
        }
}
