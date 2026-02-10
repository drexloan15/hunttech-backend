package com.comutel.backend.controller;

import com.comutel.backend.model.Usuario;
import com.comutel.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // Importante para respuestas HTTP
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // 1. Listar
    @GetMapping
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    // 2. Crear (CORREGIDO)
    @PostMapping
    public Usuario crearUsuario(@RequestBody Usuario usuario) {
        // Validación básica: Si no trae rol, le ponemos CLIENTE por defecto
        if (usuario.getRol() == null) {
            usuario.setRol(Usuario.Rol.CLIENTE);
        }
        return usuarioRepository.save(usuario);
    }

    // 3. Eliminar (CORREGIDO CON PROTECCIÓN)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            usuarioRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Esto pasa si intentas borrar un usuario que tiene tickets
            return ResponseEntity.badRequest().body("No se puede eliminar: El usuario tiene tickets asociados.");
        }
    }

    // 4. Kit de Emergencia (Ya lo tenías, déjalo ahí)
    @GetMapping("/reparar-admin/{email}/{password}")
    public Usuario repararAdmin(@PathVariable String email, @PathVariable String password) {
        Optional<Usuario> existente = usuarioRepository.findByEmail(email);
        Usuario u = existente.orElse(new Usuario());
        u.setEmail(email);
        u.setNombre("Super Admin");
        u.setPassword(password);
        u.setRol(Usuario.Rol.ADMIN);
        return usuarioRepository.save(u);
    }
}