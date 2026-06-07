package com.shsm.api.controller;

import com.shsm.api.dto.persona.PersonaRequest;
import com.shsm.api.dto.persona.PersonaResponse;
import com.shsm.api.service.PersonaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;

    @Value("${app.profile-pictures-dir:profile_pictures}")
    private String profilePicturesDir;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public ResponseEntity<Page<PersonaResponse>> listar(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "apPaterno") Pageable pageable) {
        return ResponseEntity.ok(personaService.listar(q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(personaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public ResponseEntity<PersonaResponse> crear(@Valid @RequestBody PersonaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public ResponseEntity<PersonaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PersonaRequest request) {
        return ResponseEntity.ok(personaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        personaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Actualiza el perfil del usuario autenticado */
    @PutMapping("/me")
    public ResponseEntity<PersonaResponse> actualizarMiPerfil(
            @Valid @RequestBody PersonaRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(personaService.actualizarMiPerfil(authentication.getName(), request));
    }

    /** Sube foto de perfil del usuario autenticado (jpg, jpeg, png) */
    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subirFoto(
            @RequestParam("foto") MultipartFile file,
            Authentication authentication) throws IOException {

        String original = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";

        if (!original.endsWith(".jpg") && !original.endsWith(".jpeg") && !original.endsWith(".png")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Solo se permiten archivos JPG, JPEG y PNG"));
        }

        String curp = personaService.obtenerCurpPorUsername(authentication.getName());
        if (curp == null || curp.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El usuario no tiene CURP registrado. Registra tu CURP antes de subir la foto."));
        }

        String ext = original.endsWith(".png") ? "png" : "jpg";

        Path dir = Paths.get(profilePicturesDir).toAbsolutePath();
        Files.createDirectories(dir);

        // Eliminar foto previa en cualquier extensión
        for (String e : List.of("jpg", "jpeg", "png")) {
            Files.deleteIfExists(dir.resolve(curp + "." + e));
        }

        Path destino = dir.resolve(curp + "." + ext);
        Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        return ResponseEntity.ok(Map.of("url", "/profile_pictures/" + curp + "." + ext));
    }

    /** Devuelve la URL de la foto de perfil del usuario autenticado */
    @GetMapping("/me/foto")
    public ResponseEntity<Map<String, String>> obtenerFoto(Authentication authentication) {
        String curp = personaService.obtenerCurpPorUsername(authentication.getName());
        if (curp != null && !curp.isBlank()) {
            Path dir = Paths.get(profilePicturesDir).toAbsolutePath();
            for (String ext : List.of("jpg", "jpeg", "png")) {
                if (Files.exists(dir.resolve(curp + "." + ext))) {
                    return ResponseEntity.ok(Map.of("url", "/profile_pictures/" + curp + "." + ext));
                }
            }
        }
        return ResponseEntity.ok(Map.of("url", ""));
    }
}