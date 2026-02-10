package com.comutel.backend.controller;

import com.comutel.backend.model.Articulo;
import com.comutel.backend.repository.ArticuloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kb")
@CrossOrigin(origins = "http://localhost:5173")
public class ArticuloController {

    @Autowired
    private ArticuloRepository articuloRepository;

    // 1. Listar Todos (o buscar si hay parametro ?query=...)
    @GetMapping
    public List<Articulo> listar(@RequestParam(required = false) String query) {
        if (query != null && !query.isEmpty()) {
            return articuloRepository.findByTituloContainingIgnoreCaseOrContenidoContainingIgnoreCase(query, query);
        }
        return articuloRepository.findAll();
    }

    // 2. Crear Articulo
    @PostMapping
    public Articulo crear(@RequestBody Articulo articulo) {
        return articuloRepository.save(articulo);
    }

    // 3. Eliminar
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        articuloRepository.deleteById(id);
    }
}