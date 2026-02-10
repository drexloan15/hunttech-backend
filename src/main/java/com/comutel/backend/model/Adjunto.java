package com.comutel.backend.model;

import jakarta.persistence.*;
import lombok.Data; // Si usas Lombok, si no, pon getters/setters
import java.time.LocalDateTime;

@Entity
@Data
public class Adjunto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreArchivo;
    private String tipoContenido; // ej: image/png, application/pdf
    private String url; // Ruta de acceso o URL pública

    private LocalDateTime fechaSubida;

    @ManyToOne
    private Ticket ticket;

    @ManyToOne
    private Usuario subidoPor;

    public Adjunto() {
        this.fechaSubida = LocalDateTime.now();
    }
}