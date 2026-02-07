package com.comutel.backend.service;

import com.comutel.backend.model.Comentario;
import com.comutel.backend.model.Ticket;
import com.comutel.backend.model.Usuario;
import com.comutel.backend.repository.ComentarioRepository;
import com.comutel.backend.repository.TicketRepository;
import com.comutel.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ComentarioRepository comentarioRepository; // <--- Nuevo para comentarios

    @Autowired
    private EmailSenderService emailSenderService;

    // --- 1. CREAR TICKET ---
    @Transactional
    public Ticket crearTicket(Ticket ticket) {
        if (ticket.getUsuario() == null || ticket.getUsuario().getId() == null) {
            throw new RuntimeException("Error: El ticket no tiene usuario asignado.");
        }

        Usuario usuario = usuarioRepository.findById(ticket.getUsuario().getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        ticket.setUsuario(usuario);
        ticket.setEstado(Ticket.Estado.NUEVO);
        ticket.setTecnico(null);

        Ticket ticketGuardado = ticketRepository.save(ticket);

        // Notificaciones
        enviarCorreoCreacion(ticketGuardado, usuario);

        return ticketGuardado;
    }

    // --- 2. ATENDER TICKET ---
    @Transactional
    public Ticket atenderTicket(Long ticketId, Long tecnicoId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Usuario tecnico = usuarioRepository.findById(tecnicoId)
                .orElseThrow(() -> new RuntimeException("Técnico no encontrado"));

        ticket.setTecnico(tecnico);
        ticket.setEstado(Ticket.Estado.EN_PROCESO);

        return ticketRepository.save(ticket);
    }

    // --- 3. FINALIZAR TICKET ---
    @Transactional
    public Ticket finalizarTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        ticket.setEstado(Ticket.Estado.RESUELTO);
        Ticket ticketGuardado = ticketRepository.save(ticket);

        // Enviar correo de resolución
        enviarCorreoResolucion(ticketGuardado);

        return ticketGuardado;
    }

    // --- 4. COMENTARIOS (Lógica movida aquí) ---
    public List<Comentario> obtenerComentarios(Long ticketId) {
        return comentarioRepository.findByTicketId(ticketId);
    }

    @Transactional
    public Comentario agregarComentario(Long ticketId, Map<String, Object> payload) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Long autorId = Long.valueOf(payload.get("autorId").toString());
        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String texto = payload.get("texto").toString();

        // Manejo de imagen
        String imagen = null;
        if (payload.containsKey("imagen") && payload.get("imagen") != null) {
            String posibleImagen = payload.get("imagen").toString();
            if (!posibleImagen.isEmpty() && !posibleImagen.equals("null")) {
                imagen = posibleImagen;
            }
        }

        Comentario nuevoComentario = new Comentario(texto, autor, ticket, imagen);
        return comentarioRepository.save(nuevoComentario);
    }

    // --- 5. MÉTRICAS ---
    public Map<String, Long> obtenerMetricas() {
        Map<String, Long> metricas = new HashMap<>();
        metricas.put("total", ticketRepository.count());
        metricas.put("nuevos", ticketRepository.countByEstado(Ticket.Estado.NUEVO));
        metricas.put("proceso", ticketRepository.countByEstado(Ticket.Estado.EN_PROCESO));
        metricas.put("resueltos", ticketRepository.countByEstado(Ticket.Estado.RESUELTO));
        return metricas;
    }

    // --- AUXILIARES (Correos y Búsquedas) ---
    public List<Ticket> obtenerTodos() { return ticketRepository.findAll(); }
    public Ticket obtenerPorId(Long id) { return ticketRepository.findById(id).orElseThrow(() -> new RuntimeException("Ticket no encontrado")); }

    private void enviarCorreoCreacion(Ticket ticket, Usuario usuario) {
        try {
            emailSenderService.enviarNotificacion(usuario.getEmail(), "Ticket #" + ticket.getId(), "Recibimos tu solicitud: " + ticket.getTitulo());
            emailSenderService.enviarNotificacion("jean.puccio@comutelperu.com", "🚨 Nuevo Ticket", "Cliente: " + usuario.getNombre());
        } catch (Exception e) { System.err.println("Error email: " + e.getMessage()); }
    }

    private void enviarCorreoResolucion(Ticket ticket) {
        try {
            emailSenderService.enviarNotificacion(ticket.getUsuario().getEmail(), "Ticket Resuelto", "Tu ticket ha sido resuelto. Gracias.");
        } catch (Exception e) { System.err.println("Error email: " + e.getMessage()); }
    }
}