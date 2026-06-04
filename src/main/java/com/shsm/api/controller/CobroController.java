package com.shsm.api.controller;

import com.shsm.api.entity.CobroProgramado;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.CobroProgramadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cobros")
@RequiredArgsConstructor
public class CobroController {

    private final CobroProgramadoRepository cobroRepository;

    @GetMapping("/contrato/{contratoId}")
    public ResponseEntity<List<CobroProgramado>> listarPorContrato(
            @PathVariable Long contratoId) {
        return ResponseEntity.ok(
                cobroRepository.findByContratoIdOrderByNumeroMensualidad(contratoId));
    }

    @GetMapping("/contrato/{contratoId}/pendientes")
    public ResponseEntity<List<CobroProgramado>> pendientesPorContrato(
            @PathVariable Long contratoId) {
        return ResponseEntity.ok(
                cobroRepository.findPendientesByContrato(contratoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CobroProgramado> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(cobroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CobroProgramado", id)));
    }
}
