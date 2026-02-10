package com.comutel.backend.dto;
import com.comutel.backend.model.Activo;

import com.comutel.backend.model.Ticket;

import java.util.List;

public class TicketDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private String estado;      // Lo pasamos como String
    private String prioridad;   // NUEVO: ALTA, MEDIA, BAJA
    private String categoria;   // NUEVO: Nombre de la categoría
    private String fechaCreacion;
    private String fechaVencimiento;// NUEVO: Para saber el SLA
    private String grupoAsignado; // <--- NUEVO: Solo enviamos el nombre del grupo

    private UsuarioDTO usuario;
    private UsuarioDTO tecnico;
    private List<Activo> activos;

    public TicketDTO() {}

    // --- GETTERS Y SETTERS ---
    public List<Activo> getActivos() { return activos; }
    public void setActivos(List<Activo> activos) { this.activos = activos; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(String fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public UsuarioDTO getUsuario() { return usuario; }
    public void setUsuario(UsuarioDTO usuario) { this.usuario = usuario; }

    public UsuarioDTO getTecnico() { return tecnico; }
    public void setTecnico(UsuarioDTO tecnico) { this.tecnico = tecnico; }

    public String getGrupoAsignado() { return grupoAsignado; }
    public void setGrupoAsignado(String grupoAsignado) { this.grupoAsignado = grupoAsignado; }
}