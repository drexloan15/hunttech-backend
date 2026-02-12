package com.comutel.backend.controller;

import com.comutel.backend.dto.TicketDTO;
import com.comutel.backend.model.*;
import com.comutel.backend.repository.ActivoRepository;
import com.comutel.backend.repository.ComentarioRepository; // 👈 Importante
import com.comutel.backend.repository.UsuarioRepository;    // 👈 Importante
import com.comutel.backend.service.TicketService;
import com.comutel.backend.service.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
// ⚠️ SIN @CrossOrigin (Ya lo maneja WebConfig globalmente)
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private EmailSenderService emailService;

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository; // 👈 Necesario para crear tickets

    @Autowired
    private ComentarioRepository comentarioRepository; // 👈 Necesario para el chat

    // 1. Crear (CORREGIDO: Maneja usuarioId manualmente)
    @PostMapping
    public TicketDTO crearTicket(@RequestBody Map<String, Object> payload) {
        Ticket ticket = new Ticket();
        ticket.setTitulo((String) payload.get("titulo"));
        ticket.setDescripcion((String) payload.get("descripcion"));

        String prioridadStr = (String) payload.get("prioridad");
        if(prioridadStr != null) {
            ticket.setPrioridad(Ticket.Prioridad.valueOf(prioridadStr));
        }

        // Buscamos al usuario por ID
        Object userIdObj = payload.get("usuarioId");
        if (userIdObj != null) {
            Long usuarioId = Long.valueOf(userIdObj.toString());
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            ticket.setUsuario(usuario);
        } else {
            throw new RuntimeException("Error: usuarioId es obligatorio");
        }

        return ticketService.crearTicket(ticket);
    }

    // 2. Ver Todos (Asegúrate que tu servicio tenga listarTodos o obtenerTodos)
    @GetMapping
    public List<Ticket> obtenerTodos() {
        return ticketService.listarTodos();
    }

    // 3. Atender
    @PutMapping("/{id}/atender/{tecnicoId}")
    public TicketDTO atenderTicket(@PathVariable Long id, @PathVariable Long tecnicoId) {
        return ticketService.atenderTicket(id, tecnicoId);
    }

    // 4. Finalizar
    @PutMapping("/{id}/finalizar")
    public TicketDTO finalizarTicket(@PathVariable Long id, @RequestBody String notaCierre) {
        return ticketService.finalizarTicket(id, notaCierre);
    }

    // 5. Métricas
    @GetMapping("/metricas")
    public Map<String, Long> obtenerMetricas() {
        return ticketService.obtenerMetricas();
    }

    // 6. Ver Comentarios (CORREGIDO: Directo al Repo para evitar 404)
    @GetMapping("/{id}/comentarios")
    public List<Comentario> verComentarios(@PathVariable Long id) {
        return comentarioRepository.findByTicketId(id);
    }

    // 7. Agregar Comentario
    @PostMapping("/{id}/comentarios")
    public Comentario agregarComentario(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return ticketService.agregarComentario(id, payload);
    }

    // 8. Asignar Grupo
    @PutMapping("/{id}/asignar-grupo/{grupoId}")
    public TicketDTO asignarGrupo(@PathVariable Long id, @PathVariable Long grupoId, @RequestParam Long actorId) {
        return ticketService.asignarGrupo(id, grupoId, actorId);
    }

    // 9. Historial
    @GetMapping("/{id}/historial")
    public List<HistorialTicket> obtenerHistorial(@PathVariable Long id) {
        return ticketService.obtenerHistorial(id);
    }

    // 10. Obtener por ID
    @GetMapping("/{id}")
    public TicketDTO obtenerPorId(@PathVariable Long id) {
        return ticketService.obtenerTicketDTO(id);
    }

    // 11. Iniciar Chat
    @PostMapping("/{id}/iniciar-chat")
    public void notificarInicioChat(@PathVariable Long id, @RequestParam Long usuarioId) {
        ticketService.iniciarChat(id, usuarioId);
    }

    // 12. Enviar Correo Manual
    @PostMapping("/{id}/enviar-correo")
    public void enviarCorreoManual(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        TicketDTO ticket = ticketService.obtenerTicketDTO(id);
        if(ticket.getUsuario() != null) {
            emailService.enviarNotificacion(ticket.getUsuario().getEmail(), payload.get("asunto"), payload.get("mensaje"));
        }
    }

    // 13. Vincular Activo
    @PutMapping("/{id}/vincular-activo/{activoId}")
    public TicketDTO vincularActivo(@PathVariable Long id, @PathVariable Long activoId) {
        return ticketService.vincularActivo(id, activoId);
    }

    // 14. Listar Activos
    @GetMapping("/activos")
    public List<Activo> listarActivos() {
        return activoRepository.findAll();
    }

    // 15. Crear Activo
    @PostMapping("/activos")
    public Activo crearActivo(@RequestBody Activo activo) {
        return activoRepository.save(activo);
    }

    // 16. Asignar Técnico (Endpoint faltante que agregaste al final)
    @PutMapping("/{id}/asignar/{tecnicoId}")
    public ResponseEntity<TicketDTO> asignarTecnico(@PathVariable Long id, @PathVariable Long tecnicoId) {
        TicketDTO ticketActualizado = ticketService.asignarTecnico(id, tecnicoId);
        return ResponseEntity.ok(ticketActualizado);
    }
}