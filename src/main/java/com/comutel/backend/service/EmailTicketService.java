package com.comutel.backend.service;

import com.comutel.backend.model.Ticket;
import com.comutel.backend.model.Usuario;
import com.comutel.backend.repository.TicketRepository;
import com.comutel.backend.repository.UsuarioRepository;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailTicketService {
    private static final String MISSING_IMAP_CONFIG_MSG =
            "IMAP deshabilitado: configura app.imap.user y app.imap.password para procesar correos.";

    private boolean missingImapConfigWarningLogged = false;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${app.imap.host}")
    private String imapHost;

    @Value("${app.imap.port}")
    private String imapPort;

    @Value("${app.imap.user}")
    private String imapUser;

    @Value("${app.imap.password}")
    private String imapPassword;

    @Scheduled(fixedRate = 120000)
    public void revisarCorreo() {
        System.out.println("Robot: Revisando bandeja de entrada...");

        String host = limpiar(imapHost);
        String port = limpiar(imapPort);
        String user = limpiar(imapUser);
        String password = limpiar(imapPassword).replaceAll("\\s+", "");

        if (host.isEmpty() || port.isEmpty() || user.isEmpty() || password.isEmpty()) {
            if (!missingImapConfigWarningLogged) {
                System.out.println(MISSING_IMAP_CONFIG_MSG);
                missingImapConfigWarningLogged = true;
            }
            return;
        }

        missingImapConfigWarningLogged = false;

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imap.host", host);
        props.put("mail.imap.port", port);
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.starttls.enable", "true");
        // Gmail can reject malformed PLAIN auth payloads; LOGIN is more tolerant.
        props.put("mail.imap.auth.mechanisms", "LOGIN");
        props.put("mail.imaps.auth.mechanisms", "LOGIN");

        try {
            Session session = Session.getDefaultInstance(props, null);
            Store store = session.getStore("imaps");
            store.connect(host, user, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            for (Message message : messages) {
                crearTicketDesdeCorreo(message);
                message.setFlag(Flags.Flag.SEEN, true);
            }

            inbox.close(false);
            store.close();

        } catch (MessagingException e) {
            System.out.println("Error IMAP al conectar: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void crearTicketDesdeCorreo(Message message) throws Exception {
        String titulo = message.getSubject();
        String remitente = ((InternetAddress) message.getFrom()[0]).getAddress();

        Usuario usuarioCliente = usuarioRepository.findByEmail(remitente).orElse(null);

        if (usuarioCliente == null) {
            System.out.println("ALERTA: Correo rechazado de " + remitente + " (No es usuario registrado).");
            return;
        }

        System.out.println("Usuario verificado: " + usuarioCliente.getNombre());

        String descripcionSucia = "Enviado por email";
        if (message.getContent() instanceof String) {
            descripcionSucia = (String) message.getContent();
        } else if (message.getContent() instanceof Multipart multipart) {
            BodyPart part = multipart.getBodyPart(0);
            descripcionSucia = part.getContent().toString();
        }

        String descripcionLimpia = descripcionSucia.replaceAll("\\<.*?\\>", "");
        descripcionLimpia = limpiarCuerpoCorreo(descripcionLimpia);

        Ticket ticket = new Ticket();
        ticket.setTitulo(titulo);
        ticket.setDescripcion(descripcionLimpia);
        ticket.setPrioridad(Ticket.Prioridad.MEDIA);
        ticket.calcularVencimiento();
        ticket.setEstado(Ticket.Estado.NUEVO);
        ticket.setUsuario(usuarioCliente);
        ticket.setTecnico(null);

        ticketRepository.save(ticket);
        System.out.println("Ticket creado exitosamente para: " + usuarioCliente.getNombre());
    }

    private String limpiarCuerpoCorreo(String texto) {
        if (texto == null) return "";

        String[] separadores = {
                "--",
                "Saludos",
                "Cordialmente",
                "Atentamente",
                "Aviso Legal",
                "De:",
                "From:"
        };

        for (String separador : separadores) {
            if (texto.contains(separador)) {
                texto = texto.substring(0, texto.indexOf(separador));
            }
        }

        return texto.trim();
    }

    private String limpiar(String value) {
        return value == null ? "" : value.trim();
    }
}
