package com.comutel.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    @Autowired
    private JavaMailSender mailSender; // Herramienta de Spring para enviar

    public void enviarNotificacion(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("TU_CORREO@gmail.com"); // Quien lo envía
            message.setTo(destinatario);            // A quien le llega
            message.setSubject(asunto);
            message.setText(cuerpo);

            mailSender.send(message); // 🚀 ¡Fiuuu! Sale el correo
            System.out.println("📧 Correo enviado a: " + destinatario);
        } catch (Exception e) {
            System.err.println("❌ Error enviando correo: " + e.getMessage());
        }
    }
}