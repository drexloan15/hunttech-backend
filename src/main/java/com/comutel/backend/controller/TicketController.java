package com.comutel.backend.controller;

import com.comutel.backend.model.Comentario;
import com.comutel.backend.model.Ticket;
import com.comutel.backend.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "http://localhost:5173")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    // 1. Crear
    @PostMapping
    public Ticket crearTicket(@RequestBody Ticket ticket) {
        return ticketService.crearTicket(ticket);
    }

    // 2. Ver Todos
    @GetMapping
    public List<Ticket> obtenerTodos() {
        return ticketService.obtenerTodos();
    }

    // 3. Atender (Asignar técnico)
    @PutMapping("/{id}/atender/{tecnicoId}")
    public Ticket atenderTicket(@PathVariable Long id, @PathVariable Long tecnicoId) {
        return ticketService.atenderTicket(id, tecnicoId);
    }

    // 4. Finalizar
    @PutMapping("/{id}/finalizar")
    public Ticket finalizarTicket(@PathVariable Long id) {
        return ticketService.finalizarTicket(id);
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
}