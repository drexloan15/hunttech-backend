package com.comutel.backend.repository;

import com.comutel.backend.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    // Método mágico para buscar todos los comentarios de un Ticket específico
    List<Comentario> findByTicketId(Long ticketId);
}