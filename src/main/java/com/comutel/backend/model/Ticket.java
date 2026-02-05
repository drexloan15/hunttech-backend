package com.comutel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT") // Recuerda que cambiamos esto a TEXT
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    private LocalDateTime fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // El Cliente (Dueño del problema)

    // --- AGREGAMOS ESTO (El Técnico) ---
    @ManyToOne
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico; // El Técnico (Quien lo resuelve)

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (estado == null) {
            estado = Estado.NUEVO; // Por defecto nace como NUEVO
        }
    }

    public enum Prioridad {
        BAJA, MEDIA, ALTA
    }

    // --- ACTUALIZAMOS LA LISTA DE ESTADOS AQUÍ ---
    public enum Estado {
        NUEVO,      // Recién llegado
        EN_PROCESO, // Un técnico lo tomó
        RESUELTO,   // El técnico dice que ya quedó
        CERRADO     // Confirmado y archivado
    }
}