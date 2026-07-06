package com.shsm.api.controller;

import com.shsm.api.entity.Documento;
import com.shsm.api.entity.Persona;
import com.shsm.api.entity.catalog.TipoDocumento;
import com.shsm.api.exception.BusinessException;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.DocumentoRepository;
import com.shsm.api.repository.PersonaRepository;
import com.shsm.api.repository.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoRepository      documentoRepository;
    private final PersonaRepository        personaRepository;
    private final TipoDocumentoRepository  tipoDocumentoRepository;

    @Value("${app.docs-dir:docs}")
    private String docsDir;

    private static final Set<String> TIPOS_REQUERIDOS = Set.of("INE", "CURP", "COMPROBANTE_DOM", "ACTA_NAC", "RFC");
    private static final Set<String> MIME_PERMITIDOS = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "application/pdf"
    );

    /** Subir o reemplazar un documento para una persona. */
    @PostMapping("/persona/{personaId}")
    public ResponseEntity<Map<String, Object>> subir(
            @PathVariable Long personaId,
            @RequestParam String clave,
            @RequestParam("archivo") MultipartFile archivo) throws IOException {

        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", personaId));

        TipoDocumento tipo = tipoDocumentoRepository.findByClave(clave.toUpperCase())
                .orElseThrow(() -> new BusinessException("Tipo de documento inválido: " + clave));

        String mime = archivo.getContentType();
        if (mime == null || !MIME_PERMITIDOS.contains(mime.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Formato no permitido. Use JPG, PNG, WEBP o PDF."));
        }

        String ext = resolverExtension(mime, archivo.getOriginalFilename());
        Path dir = Paths.get(docsDir, String.valueOf(personaId)).toAbsolutePath();
        Files.createDirectories(dir);

        // Eliminar versiones previas del mismo tipo
        for (String e : List.of("jpg","jpeg","png","webp","pdf")) {
            Files.deleteIfExists(dir.resolve(clave.toUpperCase() + "." + e));
        }

        String nombreArchivo = clave.toUpperCase() + "." + ext;
        Path destino = dir.resolve(nombreArchivo);
        Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        // Actualizar o crear registro en BD
        List<Documento> existentes = documentoRepository.findByPersonaIdAndActivoTrue(personaId)
                .stream().filter(d -> d.getTipo().getClave().equals(clave.toUpperCase())).toList();
        existentes.forEach(d -> { d.setActivo(false); documentoRepository.save(d); });

        Documento doc = new Documento();
        doc.setPersona(persona);
        doc.setTipo(tipo);
        doc.setNombreArchivo(nombreArchivo);
        doc.setRutaAlmacenamiento(destino.toString());
        doc.setMimeType(mime);
        doc.setTamanoBytes(archivo.getSize());
        documentoRepository.save(doc);

        log.info("Documento {} subido para persona {}", clave, personaId);
        return ResponseEntity.ok(Map.of(
                "clave", clave.toUpperCase(),
                "nombre", tipo.getNombre(),
                "url", "/docs/" + personaId + "/" + nombreArchivo
        ));
    }

    /** Lista los documentos subidos de una persona. */
    @GetMapping("/persona/{personaId}")
    public ResponseEntity<List<Map<String, Object>>> listar(@PathVariable Long personaId) {
        List<Map<String, Object>> docs = documentoRepository.findByPersonaIdAndActivoTrue(personaId)
                .stream()
                .map(d -> Map.<String, Object>of(
                        "id",     d.getId(),
                        "clave",  d.getTipo().getClave(),
                        "nombre", d.getTipo().getNombre(),
                        "url",    "/docs/" + personaId + "/" + d.getNombreArchivo(),
                        "mime",   d.getMimeType() != null ? d.getMimeType() : ""
                ))
                .toList();
        return ResponseEntity.ok(docs);
    }

    /** Indica qué tipos requeridos faltan para crear contrato. */
    @GetMapping("/persona/{personaId}/pendientes")
    public ResponseEntity<Map<String, Object>> pendientes(@PathVariable Long personaId) {
        Set<String> subidos = documentoRepository.findByPersonaIdAndActivoTrue(personaId)
                .stream()
                .map(d -> d.getTipo().getClave())
                .collect(java.util.stream.Collectors.toSet());

        List<String> faltantes = TIPOS_REQUERIDOS.stream()
                .filter(t -> !subidos.contains(t))
                .sorted()
                .toList();

        return ResponseEntity.ok(Map.of(
                "completo",  faltantes.isEmpty(),
                "faltantes", faltantes
        ));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String resolverExtension(String mime, String originalName) {
        if ("application/pdf".equals(mime)) return "pdf";
        if ("image/png".equals(mime))       return "png";
        if ("image/webp".equals(mime))      return "webp";
        if (originalName != null && originalName.toLowerCase().endsWith(".jpeg")) return "jpeg";
        return "jpg";
    }

    /** Guarda el video de consentimiento de una persona en docs/{personaId}/. */
    @PostMapping("/persona/{personaId}/video-consentimiento")
    public ResponseEntity<Map<String, Object>> subirVideoConsentimiento(
            @PathVariable Long personaId,
            @RequestParam("video") MultipartFile video) throws IOException {

        if (!personaRepository.existsById(personaId)) {
            throw new ResourceNotFoundException("Persona", personaId);
        }

        String mime = video.getContentType() != null ? video.getContentType().toLowerCase() : "";
        String fn   = video.getOriginalFilename() != null ? video.getOriginalFilename().toLowerCase() : "";
        boolean esVideo = mime.startsWith("video/") || mime.equals("application/octet-stream")
                       || fn.endsWith(".webm") || fn.endsWith(".mp4") || fn.endsWith(".ogv");
        if (!esVideo) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Formato de video no permitido."));
        }

        String ext = (mime.contains("mp4") || fn.endsWith(".mp4")) ? "mp4"
                   : (mime.contains("ogg") || fn.endsWith(".ogv"))  ? "ogv"
                   : "webm";

        Path dir = Paths.get(docsDir, String.valueOf(personaId)).toAbsolutePath();
        Files.createDirectories(dir);

        for (String e : List.of("webm", "mp4", "ogv")) {
            Files.deleteIfExists(dir.resolve("VIDEO_CONSENT." + e));
        }

        String nombre = "VIDEO_CONSENT." + ext;
        Files.copy(video.getInputStream(), dir.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);

        log.info("Video de consentimiento guardado para persona {}", personaId);
        return ResponseEntity.ok(Map.of("url", "/docs/" + personaId + "/" + nombre));
    }

    /** Elimina (desactiva) todos los documentos activos de un tipo para una persona. */
    @DeleteMapping("/persona/{personaId}/clave/{clave}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long personaId,
            @PathVariable String clave) {

        if (!personaRepository.existsById(personaId)) {
            throw new ResourceNotFoundException("Persona", personaId);
        }

        List<Documento> docs = documentoRepository.findByPersonaIdAndActivoTrue(personaId)
                .stream()
                .filter(d -> d.getTipo().getClave().equals(clave.toUpperCase()))
                .toList();

        if (docs.isEmpty()) {
            throw new ResourceNotFoundException("Documento " + clave.toUpperCase(), personaId);
        }

        docs.forEach(d -> {
            d.setActivo(false);
            try {
                Files.deleteIfExists(Paths.get(d.getRutaAlmacenamiento()));
            } catch (IOException e) {
                log.warn("No se pudo eliminar archivo físico: {}", d.getRutaAlmacenamiento());
            }
            documentoRepository.save(d);
        });

        log.info("Documento {} eliminado para persona {}", clave.toUpperCase(), personaId);
        return ResponseEntity.noContent().build();
    }
}
