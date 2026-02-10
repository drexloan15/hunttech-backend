package com.comutel.backend.service;

import com.comutel.backend.dto.TicketDTO;
import com.comutel.backend.dto.UsuarioDTO;
import com.comutel.backend.repository.UsuarioRepository;
import com.comutel.backend.model.*;
import com.comutel.backend.pattern.TicketState;
import com.comutel.backend.pattern.TicketStateFactory;
import com.comutel.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TicketService {

    @Autowired
    private GrupoResolutorRepository grupoRepository;

    @Autowired
    private HistorialTicketRepository historialRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private EmailSenderService emailSenderService;

    // 🏭 INYECCIÓN DE LA FÁBRICA (El cerebro de los estados)
    @Autowired
    private TicketStateFactory stateFactory;



    // --- 1. CREAR TICKET ---
    @Transactional
    public TicketDTO crearTicket(Ticket ticket) {
        if (ticket.getUsuario() == null || ticket.getUsuario().getId() == null) {
            throw new RuntimeException("Error: El ticket no tiene usuario asignado.");
        }

        Usuario usuario = usuarioRepository.findById(ticket.getUsuario().getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        ticket.setUsuario(usuario);
        ticket.setEstado(Ticket.Estado.NUEVO);
        ticket.setTecnico(null);

        // Calcular SLA y Prioridad
        if (ticket.getPrioridad() == null) ticket.setPrioridad(Ticket.Prioridad.BAJA);
        ticket.calcularVencimiento();

        Ticket ticketGuardado = ticketRepository.save(ticket);
        enviarCorreoCreacion(ticketGuardado, usuario);

        // 📝 Auditoría inicial
        registrarHistorial(ticketGuardado, usuario, "CREACIÓN", "Ticket creado en el sistema");

        return convertirADTO(ticketGuardado);
    }

    // --- 2. ATENDER TICKET (Con Auditoría) ---
    @Transactional
    public TicketDTO atenderTicket(Long ticketId, Long tecnicoId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Usuario tecnico = usuarioRepository.findById(tecnicoId)
                .orElseThrow(() -> new RuntimeException("Técnico no encontrado"));

        // A. Obtenemos el comportamiento del estado actual
        TicketState estadoActual = stateFactory.getState(ticket.getEstado());

        // B. Ejecutamos las reglas de negocio
        estadoActual.asignarTecnico(ticket, tecnico, tecnico);
        estadoActual.siguiente(ticket, tecnico);

        Ticket ticketActualizado = ticketRepository.save(ticket);

        // 📝 Auditoría: Guardamos que el técnico lo tomó
        registrarHistorial(ticket, tecnico, "ATENCIÓN", "Técnico " + tecnico.getNombre() + " inició la atención.");

        return convertirADTO(ticketActualizado);
    }

    // --- 3. FINALIZAR TICKET (CORREGIDO - FORZANDO ESTADO) ---
    @Transactional
    public TicketDTO finalizarTicket(Long ticketId, String notaCierre) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        // 1. Validar que no esté cerrado ya
        if (ticket.getEstado() == Ticket.Estado.RESUELTO || ticket.getEstado() == Ticket.Estado.CERRADO) {
            throw new RuntimeException("El ticket ya está resuelto.");
        }

        // 2. FORZAMOS EL CAMBIO DE ESTADO DIRECTAMENTE (Saltamos el StatePattern por seguridad)
        ticket.setEstado(Ticket.Estado.RESUELTO);

        // 3. Obtener el técnico (actor)
        Usuario actor = ticket.getTecnico();

        // 4. Guardar cambios
        Ticket ticketGuardado = ticketRepository.save(ticket);

        // 5. Enviar correo (Opcional, si falla no detiene el proceso)
        try {
            enviarCorreoResolucion(ticketGuardado);
        } catch (Exception e) {
            System.err.println("No se pudo enviar correo: " + e.getMessage());
        }

        // 6. Auditoría: Guardamos la nota técnica
        String detalleAuditoria = "Ticket resuelto. Solución técnica: " + notaCierre;
        registrarHistorial(ticket, actor, "RESOLUCIÓN", detalleAuditoria);

        return convertirADTO(ticketGuardado);
    }

    // --- 4. NUEVA FUNCIONALIDAD: ASIGNAR A GRUPO ---
    @Transactional
    public TicketDTO asignarGrupo(Long ticketId, Long grupoId, Long usuarioActorId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        GrupoResolutor grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        Usuario actor = usuarioRepository.findById(usuarioActorId)
                .orElseThrow(() -> new RuntimeException("Usuario actor no encontrado"));

        // Lógica de Negocio
        ticket.setGrupoAsignado(grupo);
        ticket.setTecnico(null); // Al cambiar de grupo, se limpia el técnico anterior
        ticket.setEstado(Ticket.Estado.EN_PROCESO); // Pasa a proceso automáticamente

        Ticket ticketGuardado = ticketRepository.save(ticket);

        // 📝 Auditoría: Guardar en el historial
        registrarHistorial(ticket, actor, "REASIGNACIÓN", "Ticket derivado al grupo: " + grupo.getNombre());

        return convertirADTO(ticketGuardado);
    }

    // --- 5. MÉTODO GENÉRICO: AVANZAR ESTADO ---
    @Transactional
    public TicketDTO siguienteEstado(Long ticketId, Long usuarioActorId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        Usuario actor = usuarioRepository.findById(usuarioActorId).orElseThrow();

        TicketState estadoActual = stateFactory.getState(ticket.getEstado());
        estadoActual.siguiente(ticket, actor);

        return convertirADTO(ticketRepository.save(ticket));
    }

    // --- 6. MÉTODOS AUXILIARES ---

    public List<Comentario> obtenerComentarios(Long ticketId) {
        return comentarioRepository.findByTicketId(ticketId);
    }

    @Transactional
    public Comentario agregarComentario(Long ticketId, Map<String, Object> payload) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        Long autorId = Long.valueOf(payload.get("autorId").toString());
        Usuario autor = usuarioRepository.findById(autorId).orElseThrow();
        String texto = payload.get("texto").toString();

        String imagen = null;
        if (payload.containsKey("imagen") && payload.get("imagen") != null) {
            String posibleImagen = payload.get("imagen").toString();
            if (!posibleImagen.isEmpty() && !posibleImagen.equals("null")) imagen = posibleImagen;
        }

        // Registrar comentario también como actividad si deseas, o dejarlo separado
        return comentarioRepository.save(new Comentario(texto, autor, ticket, imagen));
    }

    // 📊 DASHBOARD: MÉTRICAS AVANZADAS
    public Map<String, Long> obtenerMetricas() {
        Map<String, Long> metricas = new HashMap<>();

        // 1. Por Estado
        metricas.put("total", ticketRepository.count());
        metricas.put("nuevos", ticketRepository.countByEstado(Ticket.Estado.NUEVO));
        metricas.put("proceso", ticketRepository.countByEstado(Ticket.Estado.EN_PROCESO));
        metricas.put("resueltos", ticketRepository.countByEstado(Ticket.Estado.RESUELTO));
        metricas.put("cerrados", ticketRepository.countByEstado(Ticket.Estado.CERRADO));

        // 2. Por Prioridad (Asumiendo que tienes el Enum Prioridad.ALTA)
        // Nota: Si no tienes un método countByPrioridad en el repo, agrégalo o usa filtros de stream
        // Opción rápida con Streams si el repo no tiene el método listo:
        long criticos = ticketRepository.findAll().stream()
                .filter(t -> t.getPrioridad() == Ticket.Prioridad.ALTA && t.getEstado() != Ticket.Estado.RESUELTO)
                .count();
        metricas.put("criticos", criticos);

        return metricas;
    }

    public List<TicketDTO> obtenerTodos() {
        return ticketRepository.findAll().stream().map(this::convertirADTO).collect(Collectors.toList());
    }

    public Ticket obtenerPorId(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
    }

    // --- MÉTODO PRIVADO PARA AUDITORÍA ---
    private void registrarHistorial(Ticket ticket, Usuario actor, String accion, String detalle) {
        if (actor == null) return; // Evitar null pointer si no hay actor definido
        HistorialTicket historial = new HistorialTicket(ticket, actor, accion, detalle);
        historialRepository.save(historial);
    }

    private void enviarCorreoCreacion(Ticket ticket, Usuario usuario) {
        try {
            emailSenderService.enviarNotificacion(usuario.getEmail(), "Ticket #" + ticket.getId(), "Recibido: " + ticket.getTitulo());
            emailSenderService.enviarNotificacion("jean.puccio@comutelperu.com", "🚨 Nuevo Ticket", "Cliente: " + usuario.getNombre());
        } catch (Exception e) { System.err.println("Error email: " + e.getMessage()); }
    }

    private void enviarCorreoResolucion(Ticket ticket) {
        try {
            emailSenderService.enviarNotificacion(ticket.getUsuario().getEmail(), "Ticket Resuelto", "Tu ticket ha sido resuelto.");
        } catch (Exception e) { System.err.println("Error email: " + e.getMessage()); }
    }

    // --- CONVERTIDOR DTO (Corregido y Limpio) ---
    private TicketDTO convertirADTO(Ticket ticket) {
        TicketDTO dto = new TicketDTO();
        dto.setId(ticket.getId());
        dto.setTitulo(ticket.getTitulo());
        dto.setDescripcion(ticket.getDescripcion());
        dto.setActivos(ticket.getActivosAfectados());

        dto.setEstado(ticket.getEstado() != null ? ticket.getEstado().toString() : "NUEVO");
        dto.setPrioridad(ticket.getPrioridad() != null ? ticket.getPrioridad().toString() : "BAJA");

        if (ticket.getCategoria() != null) dto.setCategoria(ticket.getCategoria().getNombre());

        if (ticket.getGrupoAsignado() != null) {
            dto.setGrupoAsignado(ticket.getGrupoAsignado().getNombre());
        }

        dto.setFechaCreacion(ticket.getFechaCreacion() != null ? ticket.getFechaCreacion().toString() : null);
        dto.setFechaVencimiento(ticket.getFechaVencimiento() != null ? ticket.getFechaVencimiento().toString() : null);

        if (ticket.getUsuario() != null) {
            Usuario u = ticket.getUsuario();
            dto.setUsuario(new UsuarioDTO(u.getId(), u.getNombre(), u.getEmail(), u.getRol().toString()));
        }
        if (ticket.getTecnico() != null) {
            Usuario t = ticket.getTecnico();
            dto.setTecnico(new UsuarioDTO(t.getId(), t.getNombre(), t.getEmail(), t.getRol().toString()));
        }

        return dto;
    }
    public List<HistorialTicket> obtenerHistorial(Long ticketId) {
        return historialRepository.findByTicketIdOrderByFechaDesc(ticketId);
    }

    public TicketDTO obtenerTicketDTO(Long id) {
        Ticket ticket = obtenerPorId(id);
        return convertirADTO(ticket);
    }

    // --- 7. NOTIFICACIÓN DE CHAT ---
    public void iniciarChat(Long ticketId, Long usuarioId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Usuario iniciador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String mensaje = "El usuario " + iniciador.getNombre() + " ha iniciado un chat en el ticket #" + ticket.getId();
        System.out.println("📧 LOG: " + mensaje);

        // Enviar correo al dueño del ticket (si no es él quien inició)
        if (!ticket.getUsuario().getId().equals(usuarioId)) {
            emailSenderService.enviarNotificacion(
                    ticket.getUsuario().getEmail(),
                    "💬 Chat iniciado en Ticket #" + ticket.getId(),
                    "Un técnico ha iniciado el chat para atender tu solicitud."
            );
        }

        // Enviar correo al técnico asignado (si existe y no es él quien inició)
        if (ticket.getTecnico() != null && !ticket.getTecnico().getId().equals(usuarioId)) {
            emailSenderService.enviarNotificacion(
                    ticket.getTecnico().getEmail(),
                    "💬 Chat iniciado en Ticket #" + ticket.getId(),
                    "El usuario ha iniciado el chat en el ticket que atiendes."
            );
        }
    }


    @Autowired
    private ActivoRepository activoRepository; // 👈 NECESARIO PARA VINCULAR




    @Transactional
    public TicketDTO vincularActivo(Long ticketId, Long activoId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Activo activo = activoRepository.findById(activoId)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

        // Inicializar lista si es nula
        if (ticket.getActivosAfectados() == null) {
            ticket.setActivosAfectados(new java.util.ArrayList<>());
        }

        // Evitar duplicados
        if (!ticket.getActivosAfectados().contains(activo)) {
            ticket.getActivosAfectados().add(activo);
            ticketRepository.save(ticket);
        }

        return convertirADTO(ticket);
    }
    // ... dentro de TicketService.java ...

    public TicketDTO asignarTecnico(Long ticketId, Long tecnicoId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Usuario tecnico = usuarioRepository.findById(tecnicoId)
                .orElseThrow(() -> new RuntimeException("Técnico no encontrado"));

        ticket.setTecnico(tecnico);

        // Opcional: Si el ticket era NUEVO, pásalo a EN_PROCESO automáticamente
        if (ticket.getEstado() == Ticket.Estado.NUEVO) {
            ticket.setEstado(Ticket.Estado.EN_PROCESO);
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        // 📝 Auditoría
        registrarHistorial(savedTicket, tecnico, "AUTO-ASIGNACIÓN", "Técnico se auto-asignó el ticket.");

        return convertirADTO(savedTicket); // Return DTO
    }
}