package com.comutel.backend.service;

import com.comutel.backend.model.ArticuloKB;
import com.comutel.backend.model.Usuario;
import com.comutel.backend.repository.ArticuloKBRepository;
import com.comutel.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class KbService {
    @Autowired private ArticuloKBRepository kbRepo;
    @Autowired private UsuarioRepository usuarioRepo;

    public List<ArticuloKB> listar() {
        return kbRepo.findAllByOrderByFechaCreacionDesc();
    }

    public ArticuloKB guardar(Map<String, Object> payload) {
        ArticuloKB art = new ArticuloKB();

        // Si viene ID, es edición
        if (payload.get("id") != null) {
            art = kbRepo.findById(Long.valueOf(payload.get("id").toString())).orElse(new ArticuloKB());
        }

        art.setTitulo((String) payload.get("titulo"));
        art.setContenido((String) payload.get("contenido"));
        art.setCategoria((String) payload.get("categoria"));

        // Autor
        Long autorId = Long.valueOf(payload.get("autorId").toString());
        Usuario autor = usuarioRepo.findById(autorId).orElseThrow();
        art.setAutor(autor);

        return kbRepo.save(art);
    }

    public void eliminar(Long id) {
        kbRepo.deleteById(id);
    }
}