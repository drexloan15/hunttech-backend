package com.comutel.backend.controller;

import com.comutel.backend.model.*;
import com.comutel.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;

@RestController
@RequestMapping("/api/adjuntos")
@CrossOrigin(origins = "http://localhost:5173")
public class AdjuntoController {

    @Autowired private AdjuntoRepository adjuntoRepo;
    @Autowired private TicketRepository ticketRepo;
    @Autowired private UsuarioRepository usuarioRepo;

    // CARPETA DONDE SE GUARDAN LOS ARCHIVOS
    private final Path rootLocation = Paths.get("uploads");

    public AdjuntoController() {
        try { Files.createDirectories(rootLocation); } catch (IOException e) {}
    }

    // 1. Subir Archivo
    @PostMapping("/ticket/{ticketId}")
    public Adjunto subirArchivo(@PathVariable Long ticketId,
                                @RequestParam("file") MultipartFile file,
                                @RequestParam("usuarioId") Long usuarioId) throws IOException {

        // Guardar archivo en disco
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), this.rootLocation.resolve(filename));

        // Guardar referencia en BD
        Adjunto adjunto = new Adjunto();
        adjunto.setNombreArchivo(file.getOriginalFilename());
        adjunto.setTipoContenido(file.getContentType());
        adjunto.setUrl(filename); // Guardamos el nombre para recuperarlo luego
        adjunto.setTicket(ticketRepo.findById(ticketId).get());
        adjunto.setSubidoPor(usuarioRepo.findById(usuarioId).get());

        return adjuntoRepo.save(adjunto);
    }

    // 2. Listar Adjuntos de un Ticket
    @GetMapping("/ticket/{ticketId}")
    public List<Adjunto> listarPorTicket(@PathVariable Long ticketId) {
        return adjuntoRepo.findByTicketId(ticketId);
    }

    // 3. Descargar/Ver Archivo
    @GetMapping("/ver/{filename:.+}")
    public ResponseEntity<Resource> verArchivo(@PathVariable String filename) throws MalformedURLException {
        Path file = rootLocation.resolve(filename);
        Resource resource = new UrlResource(file.toUri());

        if (resource.exists() || resource.isReadable()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } else {
            throw new RuntimeException("No se puede leer el archivo: " + filename);
        }
    }
}