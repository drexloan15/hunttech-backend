package com.comutel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Data
@Table(name = "kb_articulos")
public class ArticuloKB {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 👈 Agrega esto
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private String titulo;

    @Column(columnDefinition = "TEXT") // Para guardar HTML largo
    private String contenido;

    private String categoria;



    @ManyToOne
    private Usuario autor;
}