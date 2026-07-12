package com.shsm.api.controller;

import com.shsm.api.dto.contrato.BeneficiarioRequest;
import com.shsm.api.dto.contrato.BeneficiarioResponse;
import com.shsm.api.dto.contrato.ContratoRequest;
import com.shsm.api.dto.contrato.ContratoResponse;
import com.shsm.api.service.ContratoService;
import com.shsm.api.service.PdfContratoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;
    private final PdfContratoService pdfContratoService;

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

    @GetMapping("/{id}/beneficiarios")
    public ResponseEntity<List<BeneficiarioResponse>> listarBeneficiarios(@PathVariable Long id) {
        return ResponseEntity.ok(contratoService.listarBeneficiariosDeContrato(id));
    }

    @PostMapping("/{id}/beneficiarios")
    public ResponseEntity<BeneficiarioResponse> agregarBeneficiario(
            @PathVariable Long id,
            @Valid @RequestBody BeneficiarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contratoService.agregarBeneficiario(id, request));
    }

    @PutMapping("/{id}/beneficiarios/{beneficiarioId}")
    public ResponseEntity<BeneficiarioResponse> actualizarBeneficiario(
            @PathVariable Long id,
            @PathVariable Long beneficiarioId,
            @Valid @RequestBody BeneficiarioRequest request) {
        return ResponseEntity.ok(contratoService.actualizarBeneficiario(id, beneficiarioId, request));
    }

    @DeleteMapping("/{id}/beneficiarios/{beneficiarioId}")
    public ResponseEntity<Void> eliminarBeneficiario(
            @PathVariable Long id,
            @PathVariable Long beneficiarioId) {
        contratoService.eliminarBeneficiario(id, beneficiarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE', 'CLIENTE')")
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

    /** Descarga el PDF del contrato. */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        byte[] pdf = pdfContratoService.generarPdf(id);
        ContratoResponse c = contratoService.obtener(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Contrato_" + c.numeroContrato() + ".pdf\"")
                .body(pdf);
    }

    /** Envía el PDF del contrato por correo al titular. */
    @PostMapping("/{id}/enviar-pdf")
    public ResponseEntity<Map<String, String>> enviarPdf(@PathVariable Long id) {
        pdfContratoService.enviarPorCorreo(id);
        return ResponseEntity.ok(Map.of("mensaje", "PDF enviado al correo del titular."));
    }

    /** Verifica la elegibilidad de edad de una persona para contratar. */
    @GetMapping("/elegibilidad/{personaId}")
    public ResponseEntity<Map<String, String>> verificarElegibilidad(@PathVariable Long personaId) {
        contratoService.validarElegibilidadEdad(personaId);
        return ResponseEntity.ok(Map.of("elegible", "true"));
    }
}
