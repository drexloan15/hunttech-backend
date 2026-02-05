package com.comutel.backend.controller;

import com.comutel.backend.model.Usuario;
import com.comutel.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 1. Dice: "Esta clase responde a peticiones web y devuelve datos (JSON)"
@RequestMapping("/api/usuarios") // 2. La dirección base será: http://localhost:8080/api/usuarios
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    @Autowired // 3. Inyección de Dependencias: Spring nos "regala" el repositorio listo para usar.
    private UsuarioRepository usuarioRepository;

    // Obtener todos los usuarios
    @GetMapping // 4. Responde a peticiones GET (cuando escribes la URL en el navegador)
    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll(); // Usamos el método mágico del repositorio
    }
}