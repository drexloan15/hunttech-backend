package com.comutel.backend.controller;

import com.comutel.backend.model.ArticuloKB;
import com.comutel.backend.service.KbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kb") // 👈 La ruta base es esta
public class KbController {

    @Autowired
    private KbService kbService;

    // LEER (GET /api/kb)
    @GetMapping
    public List<ArticuloKB> listar() {
        return kbService.listar();
    }

    // 👇 ESTA ES LA PARTE QUE ESTÁ FALLANDO
    // CREAR (POST /api/kb)
    @PostMapping  // 👈 Asegúrate que NO tenga nada entre paréntesis, solo @PostMapping
    public ArticuloKB crear(@RequestBody Map<String, Object> payload) {
        return kbService.guardar(payload);
    }

    // ACTUALIZAR (PUT /api/kb/{id})
    @PutMapping("/{id}")
    public ArticuloKB actualizar(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        payload.put("id", id);
        return kbService.guardar(payload);
    }

    // ELIMINAR (DELETE /api/kb/{id})
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        kbService.eliminar(id);
    }
}