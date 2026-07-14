package com.shsm.api.controller;

import com.shsm.api.dto.persona.PersonaRequest;
import com.shsm.api.dto.persona.PersonaResponse;
import com.shsm.api.service.FtpStorageService;
import com.shsm.api.service.PersonaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;
    private final FtpStorageService ftpStorageService;

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

        // Eliminar foto previa (ambas extensiones posibles)
        for (String e : List.of("jpg", "png")) {
            ftpStorageService.delete("profile_pictures/" + curp + "." + e);
        }

        String remotePath = "profile_pictures/" + curp + "." + ext;
        String url = ftpStorageService.upload(file.getInputStream(), remotePath);

        personaService.guardarFotoUrl(authentication.getName(), url);

        return ResponseEntity.ok(Map.of("url", url));
    }

    /** Devuelve la URL de la foto de perfil del usuario autenticado */
    @GetMapping("/me/foto")
    public ResponseEntity<Map<String, String>> obtenerFoto(Authentication authentication) {
        String url = personaService.obtenerFotoUrlPorUsername(authentication.getName());
        return ResponseEntity.ok(Map.of("url", url != null ? url : ""));
    }
}