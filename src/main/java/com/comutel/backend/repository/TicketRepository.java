package com.comutel.backend.repository;

import com.comutel.backend.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUsuario(Long usuarioId);
}
