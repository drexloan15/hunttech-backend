package com.comutel.backend.model;

import jakarta.persistence.*;
import lombok.Data; // Si usas Lombok, sino pon getters/setters

@Entity
@Data
@Table(name = "activos")
public class Activo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;      // Ej: Laptop Dell Latitude 5420
    private String codigo;      // Ej: NB-0045
    private String serie;       // Ej: 8H2J9K1
    private String tipo;        // HARDWARE o SOFTWARE

    // Opcional: A quién pertenece
    @ManyToOne
    private Usuario asignadoA;
}