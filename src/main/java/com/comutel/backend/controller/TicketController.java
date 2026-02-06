package com.comutel.backend.controller;

import com.comutel.backend.model.Ticket;
import com.comutel.backend.model.Usuario;
import com.comutel.backend.model.Comentario;
import com.comutel.backend.repository.TicketRepository;
import com.comutel.backend.repository.UsuarioRepository;
import com.comutel.backend.repository.ComentarioRepository;
import com.comutel.backend.service.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "http://localhost:5173")
public class TicketController {

    @Autowired
    private TicketRepository ticketRepository;
    @PostMapping
    public Ticket crearTicket(@RequestBody Ticket ticket) {
        // 1. Validaciones (Igual que antes)
        if (ticket.getUsuario() == null || ticket.getUsuario().getId() == null) {
            throw new RuntimeException("Error: El ticket no tiene usuario asignado.");
        }

        Usuario usuario = usuarioRepository.findById(ticket.getUsuario().getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        ticket.setUsuario(usuario);
        ticket.setEstado(Ticket.Estado.NUEVO);
        ticket.setTecnico(null);

        Ticket ticketGuardado = ticketRepository.save(ticket); // Guardamos

        // --- 2. NUEVO: LÓGICA DE NOTIFICACIÓN ---

        // A) Correo de confirmación para el CLIENTE
        String asuntoCliente = "Ticket Recibido #" + ticketGuardado.getId();
        String mensajeCliente = "Hola " + usuario.getNombre() + ",\n\n" +
                "Hemos recibido tu solicitud: '" + ticket.getTitulo() + "'.\n" +
                "Un técnico de Comutel la revisará pronto.\n\n" +
                "Gracias por contactarnos.";

        emailSenderService.enviarNotificacion(usuario.getEmail(), asuntoCliente, mensajeCliente);

        // B) Alerta para los TÉCNICOS (Opcional: enviar a un correo central o iterar técnicos)
        // Por ahora enviaremos una alerta al correo central de soporte
        String correoSoporte = "jean.puccio@comutelperu.com"; // <--- PON TU CORREO DE ADMIN AQUÍ
        String asuntoSoporte = "🚨 Nuevo Ticket Web #" + ticketGuardado.getId();
        String mensajeSoporte = "Cliente: " + usuario.getNombre() + "\n" +
                "Asunto: " + ticket.getTitulo() + "\n" +
                "Prioridad: " + ticket.getPrioridad();

        emailSenderService.enviarNotificacion(correoSoporte, asuntoSoporte, mensajeSoporte);

        return ticketGuardado;
    }
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
    @GetMapping("/metricas")
    public Map<String, Long> obtenerMetricas() {
        Map<String, Long> metricas = new HashMap<>();

        // Contamos usando la base de datos
        metricas.put("total", ticketRepository.count());
        metricas.put("nuevos", ticketRepository.countByEstado(Ticket.Estado.NUEVO));
        metricas.put("proceso", ticketRepository.countByEstado(Ticket.Estado.EN_PROCESO));
        metricas.put("resueltos", ticketRepository.countByEstado(Ticket.Estado.RESUELTO));

        return metricas;
    }
    @Autowired
    private ComentarioRepository comentarioRepository;

    // 1. VER COMENTARIOS
    @GetMapping("/{id}/comentarios")
    public List<Comentario> verComentarios(@PathVariable Long id) {
        return comentarioRepository.findByTicketId(id);
    }

    // 2. AGREGAR COMENTARIO (Este es el que te está fallando)
    @PostMapping("/{id}/comentarios")
    public Comentario agregarComentario(@PathVariable Long id, @RequestBody Map<String, Object> payload) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Long autorId = Long.valueOf(payload.get("autorId").toString());
        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String texto = payload.get("texto").toString();

        // --- CORRECCIÓN DE IMAGEN ---
        String imagen = null;
        if (payload.containsKey("imagen") && payload.get("imagen") != null) {
            String posibleImagen = payload.get("imagen").toString();
            // Validamos que no sea una cadena vacía o "null" texto
            if (!posibleImagen.isEmpty() && !posibleImagen.equals("null")) {
                imagen = posibleImagen;
                System.out.println("📸 IMAGEN RECIBIDA - Tamaño: " + imagen.length() + " caracteres.");
            }
        } else {
            System.out.println("📝 Mensaje de solo texto (sin imagen).");
        }

        // Creamos el comentario con los datos limpios
        Comentario nuevoComentario = new Comentario(texto, autor, ticket, imagen);

        return comentarioRepository.save(nuevoComentario);
    }

}