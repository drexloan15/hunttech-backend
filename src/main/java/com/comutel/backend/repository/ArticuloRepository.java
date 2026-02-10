package com.comutel.backend.repository;

import com.comutel.backend.model.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    // Buscador Inteligente: Busca si el texto está en el titulo O en el contenido
    // IgnoreCase hace que no importen las mayúsculas/minúsculas
    List<Articulo> findByTituloContainingIgnoreCaseOrContenidoContainingIgnoreCase(String titulo, String contenido);
}