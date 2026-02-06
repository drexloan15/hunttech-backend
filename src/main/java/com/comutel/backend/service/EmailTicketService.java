package com.comutel.backend.service;

import com.comutel.backend.model.Ticket;
import com.comutel.backend.model.Usuario;
import com.comutel.backend.repository.TicketRepository;
import com.comutel.backend.repository.UsuarioRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailTicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Ejecutar cada 60 segundos (60000 milisegundos)
    @Scheduled(fixedRate = 120000)
    public void revisarCorreo() {
        System.out.println("🤖 Robot: Revisando bandeja de entrada...");

        String host = "imap.gmail.com";
        String username = "helpdeskcomutel@gmail.com"; // Correo
        String password = "erno mocr nlhv hduc"; // Contraseña APP

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");

        try {
            Session session = Session.getDefaultInstance(props, null);
            Store store = session.getStore("imaps");
            store.connect(host, username, password);

            // Abrir la carpeta INBOX (Bandeja de entrada)
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE); // Permiso para leer y marcar como leído

            // Buscar solo correos NO LEÍDOS
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            for (Message message : messages) {
                crearTicketDesdeCorreo(message);

                // Marcar como LEÍDO para no procesarlo 2 veces
                message.setFlag(Flags.Flag.SEEN, true);
            }

            inbox.close(false);
            store.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void crearTicketDesdeCorreo(Message message) throws Exception {
        String titulo = message.getSubject();
        String remitente = ((InternetAddress) message.getFrom()[0]).getAddress(); // El email

        // --- 1. VERIFICACIÓN DE SEGURIDAD (EL PORTERO) ---
        // Buscamos si el remitente existe en la BD
        Usuario usuarioCliente = usuarioRepository.findByEmail(remitente).orElse(null);

        // Si NO existe, IGNORAMOS el correo y terminamos aquí.
        if (usuarioCliente == null) {
            System.out.println("⛔ ALERTA: Correo rechazado de " + remitente + " (No es usuario registrado).");
            return; // <--- ESTE "RETURN" DETIENE TODO. No se guarda nada.
        }

        // Si el código llega aquí, es porque el usuario SÍ existe. Seguimos...
        System.out.println("✅ Usuario verificado: " + usuarioCliente.getNombre());

        // --- 2. LIMPIEZA DEL CORREO (Igual que antes) ---
        String descripcionSucia = "Enviado por email";
        // ... (Tu lógica para obtener el texto del body sigue igual) ...
        if (message.getContent() instanceof String) {
            descripcionSucia = (String) message.getContent();
        } else if (message.getContent() instanceof Multipart) {
            Multipart multipart = (Multipart) message.getContent();
            BodyPart part = multipart.getBodyPart(0);
            descripcionSucia = part.getContent().toString();
        }

        String descripcionLimpia = descripcionSucia.replaceAll("\\<.*?\\>", "");
        descripcionLimpia = limpiarCuerpoCorreo(descripcionLimpia);

        // --- 3. CREACIÓN DEL TICKET ---
        Ticket ticket = new Ticket();
        ticket.setTitulo(titulo);
        ticket.setDescripcion(descripcionLimpia);
        ticket.setPrioridad(Ticket.Prioridad.MEDIA);

        ticket.setEstado(Ticket.Estado.NUEVO); // Estado inicial
        ticket.setUsuario(usuarioCliente);     // El dueño es el que envió el correo
        ticket.setTecnico(null);               // Aún nadie lo atiende

        ticketRepository.save(ticket);
        System.out.println("🎫 Ticket creado exitosamente para: " + usuarioCliente.getNombre());
    }
    // ✂️ MÉTODO NUEVO: Limpia la firma y el texto legal
    private String limpiarCuerpoCorreo(String texto) {
        if (texto == null) return "";

        // Lista de palabras "chivatas" que avisan que viene la firma
        String[] separadores = {
                "--",               // Separador clásico de firma
                "Saludos",          // Típico inicio de despedida
                "Cordialmente",
                "Atentamente",
                "Aviso Legal",      // Por si no hay despedida pero sí disclaimer
                "De:",              // Por si es una cadena de correos respondidos
                "From:"
        };

        // Buscamos si existe alguna de esas palabras
        for (String separador : separadores) {
            if (texto.contains(separador)) {
                // Si la encuentra, CORTA el texto justo ahí
                texto = texto.substring(0, texto.indexOf(separador));
            }
        }

        // Elimina espacios en blanco sobrantes al final y devuelve el texto limpio
        return texto.trim();
    }
}