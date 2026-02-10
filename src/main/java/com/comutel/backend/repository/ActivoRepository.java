package com.comutel.backend.repository;
import com.comutel.backend.model.Activo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivoRepository extends JpaRepository<Activo, Long> {
    // Buscador simple
    java.util.List<Activo> findByNombreContainingIgnoreCase(String nombre);
}