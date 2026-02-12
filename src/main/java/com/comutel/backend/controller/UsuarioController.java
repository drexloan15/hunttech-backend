package com.comutel.backend.controller;

import com.comutel.backend.model.Usuario;
import com.comutel.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
// ⚠️ IMPORTANTE: Quitamos @CrossOrigin de aquí para que use la Config Global (WebConfig)
// y así permitir cookies/credenciales sin errores.
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // 1. Listar
    @GetMapping
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    // 👇 ESTE ES EL MÉTODO QUE FALTABA 👇
    // 2. Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credenciales) {
        String email = credenciales.get("email");
        String password = credenciales.get("password");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        // Verificamos si existe y si la contraseña coincide
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (usuario.getPassword().equals(password)) {
                return ResponseEntity.ok(usuario);
            }
        }

        return ResponseEntity.status(401).body("Credenciales inválidas");
    }
    // 👆 ------------------------------- 👆

    // 3. Crear
    @PostMapping
    public Usuario crearUsuario(@RequestBody Usuario usuario) {
        if (usuario.getRol() == null) {
            usuario.setRol(Usuario.Rol.CLIENTE);
        }
        return usuarioRepository.save(usuario);
    }

    // 4. Eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            usuarioRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("No se puede eliminar: El usuario tiene tickets asociados.");
        }
    }

    // 5. Kit de Emergencia
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