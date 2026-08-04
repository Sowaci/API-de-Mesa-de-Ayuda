package com.helpdesk.api.repository;

import com.helpdesk.api.entity.Ticket;
import com.helpdesk.api.enums.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByCreadoPorId(Long creadorId);

    List<Ticket> findByEstadoNotAndSlaVenceEnBefore(Estado estado, LocalDateTime fecha);

    long countByEstado(Estado estado);

    @Query("select count(t) from Ticket t where t.estado = 'RESUELTO' and t.resueltoEn is not null and t.resueltoEn <= t.slaVenceEn")
    long countResueltosDentroDeSla();

    @Query("select count(t) from Ticket t where t.estado = 'RESUELTO' and t.resueltoEn is not null and t.resueltoEn > t.slaVenceEn")
    long countResueltosFueraDeSla();

    @Query("select count(t) from Ticket t where t.estado <> 'RESUELTO' and t.slaVenceEn < :ahora")
    long countVencidos(@Param("ahora") LocalDateTime ahora);
}
