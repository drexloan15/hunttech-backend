package com.comutel.backend.controller;

import com.comutel.backend.model.Ticket;
import com.comutel.backend.model.Usuario;
import com.comutel.backend.repository.TicketRepository;
import com.comutel.backend.repository.UsuarioRepository;
import com.comutel.backend.service.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "http://localhost:5173")
public class TicketController {

    @Autowired
    private TicketRepository ticketRepository;

    // Ver todos los tickets (Admin)
    @GetMapping
    public List<Ticket> obtenerTodos() {
        return ticketRepository.findAll();
    }

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailSenderService emailSenderService;

    @PutMapping("/{id}/atender/{tecnicoId}")
    public Ticket atenderTicket(@PathVariable Long id, @PathVariable Long tecnicoId) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(()-> new RuntimeException("Ticket no encontrado"));
        Usuario tecnico = usuarioRepository.findById(tecnicoId).orElseThrow(()-> new RuntimeException("Técnico no encontrado"));
        ticket.setTecnico(tecnico);
        ticket.setEstado(Ticket.Estado.EN_PROCESO);
        return ticketRepository.save(ticket);
    }

    @PutMapping("/{id}/finalizar")
    public Ticket finalizarTicket(@PathVariable Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        ticket.setEstado(Ticket.Estado.RESUELTO);
        Ticket ticketGuardado = ticketRepository.save(ticket); // Guardamos cambio en BD

        // --- 2. ENVIAR CORREO DE CONFIRMACIÓN ---
        String emailCliente = ticket.getUsuario().getEmail();
        String asunto = "Ticket Resuelto: " + ticket.getTitulo();
        String mensaje = "Hola " + ticket.getUsuario().getNombre() + ",\n\n" +
                "Tu ticket ha sido resuelto por nuestro equipo técnico.\n" +
                "Estado final: RESUELTO ✅\n\n" +
                "Gracias por confiar en COMUTEL SERVICE.";

        // ¡Disparamos el correo!
        emailSenderService.enviarNotificacion(emailCliente, asunto, mensaje);

        return ticketGuardado;
    }


}