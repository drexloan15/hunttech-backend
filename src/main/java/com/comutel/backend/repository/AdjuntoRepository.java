package com.comutel.backend.repository;
import com.comutel.backend.model.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdjuntoRepository extends JpaRepository<Adjunto, Long> {
    List<Adjunto> findByTicketId(Long ticketId);
}