package com.comutel.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comentarios")
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String texto;

    private LocalDateTime fecha;

    @ManyToOne
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(columnDefinition = "TEXT") // <--- IMPORTANTE: Permite textos gigantes
    private String imagenBase64;

    // Constructores
    public Comentario() {
        this.fecha = LocalDateTime.now();
    }

    public Comentario(String texto, Usuario autor, Ticket ticket, String imagenBase64) {
        this.texto = texto;
        this.autor = autor;
        this.ticket = ticket;
        this.fecha = LocalDateTime.now();
        this.imagenBase64 = imagenBase64;
        this.fecha = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Usuario getAutor() { return autor; }
    public void setAutor(Usuario autor) { this.autor = autor; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public String getImagenBase64() { return imagenBase64; }
    public void setImagenBase64(String imagenBase64) { this.imagenBase64 = imagenBase64; }
}