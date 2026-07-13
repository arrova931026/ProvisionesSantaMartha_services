package com.shsm.api.controller;

import com.shsm.api.dto.contrato.CobroProgramadoResponse;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.CobroProgramadoRepository;
import com.shsm.api.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cobros")
@RequiredArgsConstructor
public class CobroController {

    private final CobroProgramadoRepository cobroRepository;
    private final PagoRepository pagoRepository;

    @GetMapping("/contrato/{contratoId}")
    public ResponseEntity<List<CobroProgramadoResponse>> listarPorContrato(
            @PathVariable Long contratoId) {
        var cobros = cobroRepository.findByContratoIdOrderByNumeroMensualidad(contratoId);
        Map<Long, LocalDate> fechasPago = fetchFechasPago(cobros.stream().map(c -> c.getId()).toList());
        return ResponseEntity.ok(cobros.stream()
                .map(c -> CobroProgramadoResponse.from(c, fechasPago.get(c.getId())))
                .toList());
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
                        .map(c -> CobroProgramadoResponse.from(c, fetchFechasPago(List.of(c.getId())).get(c.getId())))
                        .orElseThrow(() -> new ResourceNotFoundException("CobroProgramado", id)));
    }

    private Map<Long, LocalDate> fetchFechasPago(List<Long> ids) {
        Map<Long, LocalDate> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        pagoRepository.findLatestFechasPagoByCobros(ids).forEach(row -> {
            Long cobroId   = (Long) row[0];
            OffsetDateTime fp = (OffsetDateTime) row[1];
            if (fp != null) map.put(cobroId, fp.toLocalDate());
        });
        return map;
    }
}
