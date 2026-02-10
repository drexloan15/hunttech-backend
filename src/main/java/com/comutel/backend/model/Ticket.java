package com.comutel.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    @Column(length = 1000) // Recomendado para descripciones largas
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    // --- CORRECCIÓN AQUÍ: ENUM CON HORAS SLA ---
    @Enumerated(EnumType.STRING)
    private Prioridad prioridad;

    public enum Prioridad {
        BAJA(48),     // 48 Horas
        MEDIA(24),    // 24 Horas
        ALTA(8),      // 8 Horas
        CRITICA(4);   // 4 Horas

        private final int horasSLA;

        Prioridad(int horasSLA) {
            this.horasSLA = horasSLA;
        }

        public int getHorasSLA() {
            return horasSLA;
        }
    }
    // -------------------------------------------

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaVencimiento;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario; // Cliente

    @ManyToOne
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico; // Técnico asignado

    @ManyToOne
    @JoinColumn(name = "grupo_id")
    private GrupoResolutor grupoAsignado;

    // Enum de Estados
    public enum Estado {
        NUEVO, EN_PROCESO, PENDIENTE, RESUELTO, CERRADO
    }

    // Constructor vacío
    public Ticket() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = Estado.NUEVO;
        // Asignamos prioridad por defecto para evitar NullPointerException al calcular
        if (this.prioridad == null) {
            this.prioridad = Prioridad.BAJA;
        }
    }

    // --- LÓGICA DE NEGOCIO ---
    public void calcularVencimiento() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        // Si no hay prioridad, asumimos BAJA
        if (this.prioridad == null) {
            this.prioridad = Prioridad.BAJA;
        }

        // Ahora sí funcionará getHorasSLA()
        this.fechaVencimiento = this.fechaCreacion.plusHours(this.prioridad.getHorasSLA());
    }
    @ManyToMany
    @JoinTable(
            name = "ticket_activos",
            joinColumns = @JoinColumn(name = "ticket_id"),
            inverseJoinColumns = @JoinColumn(name = "activo_id")
    )
    private java.util.List<Activo> activosAfectados;

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDateTime fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Usuario getTecnico() { return tecnico; }
    public void setTecnico(Usuario tecnico) { this.tecnico = tecnico; }
    public GrupoResolutor getGrupoAsignado() { return grupoAsignado; }
    public void setGrupoAsignado(GrupoResolutor grupoAsignado) { this.grupoAsignado = grupoAsignado; }
    public java.util.List<Activo> getActivosAfectados() { return activosAfectados; }
    public void setActivosAfectados(java.util.List<Activo> activosAfectados) { this.activosAfectados = activosAfectados; }
}