package com.comutel.backend.repository;

import com.comutel.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // <--- ¡No olvides importar esto!

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Esta es la línea que le faltaba a Java para entender el comando:
    Optional<Usuario> findByEmail(String email);
}