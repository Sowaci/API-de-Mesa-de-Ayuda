package com.helpdesk.api.repository;

import com.helpdesk.api.entity.TicketHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistorialRepository extends JpaRepository<TicketHistorial, Long> {

    List<TicketHistorial> findByTicketIdOrderByFechaAsc(Long ticketId);
}
