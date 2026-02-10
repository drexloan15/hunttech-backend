package com.comutel.backend.controller;

import com.comutel.backend.dto.TicketDTO;
import com.comutel.backend.model.*;
import com.comutel.backend.repository.ActivoRepository;
import com.comutel.backend.service.TicketService;
import com.comutel.backend.service.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "http://192.168.1.173:5173")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private EmailSenderService emailService;

    @Autowired
    private ActivoRepository activoRepository;

    // 1. Crear
    @PostMapping
    public TicketDTO crearTicket(@RequestBody Ticket ticket) {
        return ticketService.crearTicket(ticket);
    }

    // 2. Ver Todos
    @GetMapping
    public List<TicketDTO> obtenerTodos() {
        return ticketService.obtenerTodos();
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

    // 6. Ver Comentarios
    @GetMapping("/{id}/comentarios")
    public List<Comentario> verComentarios(@PathVariable Long id) {
        return ticketService.obtenerComentarios(id);
    }

    // 7. Agregar Comentario
    @PostMapping("/{id}/comentarios")
    public Comentario agregarComentario(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return ticketService.agregarComentario(id, payload);
    }

    // Asignar Grupo
    @PutMapping("/{id}/asignar-grupo/{grupoId}")
    public TicketDTO asignarGrupo(@PathVariable Long id, @PathVariable Long grupoId, @RequestParam Long actorId) {
        return ticketService.asignarGrupo(id, grupoId, actorId);
    }

    // Historial
    @GetMapping("/{id}/historial")
    public List<HistorialTicket> obtenerHistorial(@PathVariable Long id) {
        return ticketService.obtenerHistorial(id);
    }

    // 8. Obtener por ID
    @GetMapping("/{id}")
    public TicketDTO obtenerPorId(@PathVariable Long id) {
        return ticketService.obtenerTicketDTO(id);
    }

    // 9. Iniciar Chat
    @PostMapping("/{id}/iniciar-chat")
    public void notificarInicioChat(@PathVariable Long id, @RequestParam Long usuarioId) {
        ticketService.iniciarChat(id, usuarioId);
    }

    // 10. Enviar Correo Manual
    @PostMapping("/{id}/enviar-correo")
    public void enviarCorreoManual(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        TicketDTO ticket = ticketService.obtenerTicketDTO(id);
        String asunto = payload.get("asunto");
        String mensaje = payload.get("mensaje");
        System.out.println("📧 Enviando correo a: " + ticket.getUsuario().getEmail());
        emailService.enviarNotificacion(ticket.getUsuario().getEmail(), asunto, mensaje);
    }

    // 11. Vincular un Activo al Ticket (CORREGIDO ✅)
    @PutMapping("/{id}/vincular-activo/{activoId}")
    public TicketDTO vincularActivo(@PathVariable Long id, @PathVariable Long activoId) {
        // Ahora llamamos al servicio, NO intentamos guardar directo aquí
        return ticketService.vincularActivo(id, activoId);
    }

    // 12. Listar Activos
    @GetMapping("/activos")
    public List<Activo> listarActivos() {
        return activoRepository.findAll();
    }

    // 13. Crear Activo
    @PostMapping("/activos")
    public Activo crearActivo(@RequestBody Activo activo) {
        return activoRepository.save(activo);
    }
    // ... dentro de TicketController.java ...

    // 👇 ESTE ES EL ENDPOINT QUE FALTABA


    @PutMapping("/{id}/asignar/{tecnicoId}")
    public ResponseEntity<TicketDTO> asignarTecnico(@PathVariable Long id, @PathVariable Long tecnicoId) {
        TicketDTO ticketActualizado = ticketService.asignarTecnico(id, tecnicoId);
        return ResponseEntity.ok(ticketActualizado);
    }

}