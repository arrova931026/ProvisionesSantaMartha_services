package com.shsm.api.controller;

import com.shsm.api.dto.contrato.CobroProgramadoResponse;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.CobroProgramadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cobros")
@RequiredArgsConstructor
public class CobroController {

    private final CobroProgramadoRepository cobroRepository;

    @GetMapping("/contrato/{contratoId}")
    public ResponseEntity<List<CobroProgramadoResponse>> listarPorContrato(
            @PathVariable Long contratoId) {
        return ResponseEntity.ok(
                cobroRepository.findByContratoIdOrderByNumeroMensualidad(contratoId)
                        .stream().map(CobroProgramadoResponse::from).toList());
    }

    @GetMapping("/contrato/{contratoId}/pendientes")
    public ResponseEntity<List<CobroProgramadoResponse>> pendientesPorContrato(
            @PathVariable Long contratoId) {
        return ResponseEntity.ok(
                cobroRepository.findPendientesByContrato(contratoId)
                        .stream().map(CobroProgramadoResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CobroProgramadoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(
                cobroRepository.findById(id)
                        .map(CobroProgramadoResponse::from)
                        .orElseThrow(() -> new ResourceNotFoundException("CobroProgramado", id)));
    }
}
