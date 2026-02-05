package com.comutel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
@Data
@Entity
@Table(name= "usuarios")

public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) // No puede estar vacío
    private String nombre;

    @Column(nullable = false, unique = true) // No puede repetirse el email
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING) // Guardamos el rol como texto ("ADMIN", "CLIENTE")
    private Rol rol;

    // Un pequeño "Enum" para definir los roles permitidos
    public enum Rol {
        ADMIN,
        TECNICO,
        CLIENTE
    }

}
