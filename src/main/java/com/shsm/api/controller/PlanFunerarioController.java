package com.shsm.api.controller;

import com.shsm.api.entity.PlanFunerario;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.PlanFunerarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/planes")
@RequiredArgsConstructor
public class PlanFunerarioController {

    private final PlanFunerarioRepository planRepository;

    @GetMapping
    public ResponseEntity<List<PlanFunerario>> listar() {
        return ResponseEntity.ok(planRepository.findByActivoTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanFunerario> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanFunerario", id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanFunerario> crear(@RequestBody PlanFunerario plan) {
        plan.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(planRepository.save(plan));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanFunerario> actualizar(@PathVariable Long id,
                                                     @RequestBody PlanFunerario plan) {
        planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanFunerario", id));
        plan.setId(id);
        return ResponseEntity.ok(planRepository.save(plan));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        PlanFunerario plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanFunerario", id));
        plan.setActivo(false);
        planRepository.save(plan);
        return ResponseEntity.noContent().build();
    }
}
