package com.comutel.backend.repository;

import com.comutel.backend.model.ArticuloKB;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticuloKBRepository extends JpaRepository<ArticuloKB, Long> {
    // Buscar por título o contenido (Básico)
    List<ArticuloKB> findByTituloContainingIgnoreCaseOrContenidoContainingIgnoreCase(String t, String c);

    // Ordenar por fecha reciente
    List<ArticuloKB> findAllByOrderByFechaCreacionDesc();
}