package com.comutel.backend.controller;

import com.comutel.backend.model.Articulo;
import com.comutel.backend.repository.ArticuloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articulos")
@CrossOrigin(origins = "http://localhost:5173") // Permiso para React
public class ArticuloController {

    @Autowired
    private ArticuloRepository articuloRepository;

    // 1. Obtener todos los artículos (Para Cliente y Técnico)
    @GetMapping
    public List<Articulo> listarArticulos() {
        return articuloRepository.findAll();
    }

    // 2. Crear un artículo nuevo (Solo Técnico)
    @PostMapping
    public Articulo crearArticulo(@RequestBody Articulo articulo) {
        return articuloRepository.save(articulo);
    }

    // 3. Borrar artículo (Por si escribimos mal)
    @DeleteMapping("/{id}")
    public void eliminarArticulo(@PathVariable Long id) {
        articuloRepository.deleteById(id);
    }
}