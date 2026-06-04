package com.shsm.api.controller;

import com.shsm.api.dto.contrato.ContratoRequest;
import com.shsm.api.dto.contrato.ContratoResponse;
import com.shsm.api.service.ContratoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public ResponseEntity<Page<ContratoResponse>> listar(
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(contratoService.listar(estado, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(contratoService.obtener(id));
    }

    @GetMapping("/persona/{personaId}")
    public ResponseEntity<List<ContratoResponse>> listarPorPersona(@PathVariable Long personaId) {
        return ResponseEntity.ok(contratoService.listarPorPersona(personaId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public ResponseEntity<ContratoResponse> crear(@Valid @RequestBody ContratoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contratoService.crear(request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public ResponseEntity<ContratoResponse> actualizarEstado(
            @PathVariable Long id,
            @RequestParam String clave) {
        return ResponseEntity.ok(contratoService.actualizarEstado(id, clave));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        contratoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
