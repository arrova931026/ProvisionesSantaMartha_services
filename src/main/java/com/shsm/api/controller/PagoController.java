package com.shsm.api.controller;

import com.shsm.api.dto.pago.PagoRequest;
import com.shsm.api.dto.pago.PagoResponse;
import com.shsm.api.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public ResponseEntity<Page<PagoResponse>> listar(
            @RequestParam(required = false) Long contratoId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(pagoService.listar(contratoId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtener(id));
    }

    @GetMapping("/contrato/{contratoId}")
    public ResponseEntity<List<PagoResponse>> listarPorContrato(@PathVariable Long contratoId) {
        return ResponseEntity.ok(pagoService.listarPorContrato(contratoId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public ResponseEntity<PagoResponse> registrar(
            @Valid @RequestBody PagoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagoService.registrar(request, userDetails.getUsername()));
    }
}
