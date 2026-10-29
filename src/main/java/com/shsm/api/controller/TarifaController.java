package com.shsm.api.controller;

import com.shsm.api.dto.plan.PlanFunerarioResponse;
import com.shsm.api.dto.plan.PlanTarifaRequest;
import com.shsm.api.dto.plan.TarifaMutualistaRequest;
import com.shsm.api.entity.CatalogoArticulo;
import com.shsm.api.entity.PlanArticulo;
import com.shsm.api.entity.PlanArticuloId;
import com.shsm.api.entity.PlanFunerario;
import com.shsm.api.exception.BusinessException;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.CatalogoArticuloRepository;
import com.shsm.api.repository.PlanFunerarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;

/**
 * Gestión de tarifas y planes por tipo (MUTUALISTA, HUMANOS, MASCOTAS).
 * Solo ADMIN.
 */
@RestController
@RequestMapping("/tarifas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TarifaController {

    private final PlanFunerarioRepository planRepository;
    private final CatalogoArticuloRepository articuloRepository;

    // ── Mutualista ──────────────────────────────────────────────────────────

    /** Actualiza la aportación anual del plan mutualista. */
    @PatchMapping("/mutualista")
    @Transactional
    public ResponseEntity<Map<String, Object>> actualizarMutualista(
            @Valid @RequestBody TarifaMutualistaRequest request) {

        PlanFunerario plan = planRepository.findByTipoPlanAndActivoTrue("MUTUALISTA").stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Plan mutualista no encontrado"));

        plan.setPrecioTotal(request.aportacionAnual());
        plan.setMensualidad(request.aportacionAnual()
                .divide(java.math.BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP));
        planRepository.save(plan);

        return ResponseEntity.ok(Map.of(
                "id", plan.getId(),
                "aportacionAnual", plan.getPrecioTotal(),
                "mensualidad", plan.getMensualidad()
        ));
    }

    // ── Planes por tipo (HUMANOS / MASCOTAS) ────────────────────────────────

    /** Lista planes de un tipo (incluye inactivos, para que el admin los reactive). */
    @GetMapping("/planes")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PlanFunerarioResponse>> listarPorTipo(@RequestParam String tipo) {
        String tipoNorm = normalizarTipo(tipo);
        return ResponseEntity.ok(
                planRepository.findByTipoPlanWithArticles(tipoNorm).stream()
                        .map(PlanFunerarioResponse::from)
                        .toList());
    }

    /** Crea un plan de HUMANOS o MASCOTAS. */
    @PostMapping("/planes")
    @Transactional
    public ResponseEntity<PlanFunerarioResponse> crearPlan(
            @RequestParam String tipo,
            @Valid @RequestBody PlanTarifaRequest request) {

        String tipoNorm = normalizarTipo(tipo);
        if ("MUTUALISTA".equals(tipoNorm)) {
            throw new BusinessException("El plan mutualista es único; use PATCH /tarifas/mutualista para cambiar su precio.");
        }

        PlanFunerario plan = new PlanFunerario();
        plan.setNombre(request.nombre());
        plan.setDescripcion(request.descripcion());
        plan.setTipoPlan(tipoNorm);
        plan.setPrecioTotal(request.precioTotal());
        plan.setMensualidad(request.mensualidad());
        plan.setDuracionMeses(request.duracionMeses());
        plan.setNumeroBeneficiarios(request.numeroBeneficiarios());
        plan.setPorcentajeExtraMascota(request.porcentajeExtraMascota() != null
                ? request.porcentajeExtraMascota() : java.math.BigDecimal.ZERO);

        asignarArticulos(plan, request.articuloIds());

        if ("MUTUALISTA_MASCOTAS".equals(tipoNorm)) {
            plan.aplicarMutualistaMascotas();
            plan.getArticulos().clear();
        }

        PlanFunerario guardado = planRepository.save(plan);
        return ResponseEntity.status(HttpStatus.CREATED).body(PlanFunerarioResponse.from(guardado));
    }

    /** Actualiza un plan existente (precio, duración, beneficiarios y artículos). */
    @PutMapping("/planes/{id}")
    @Transactional
    public ResponseEntity<PlanFunerarioResponse> actualizarPlan(
            @PathVariable Long id,
            @Valid @RequestBody PlanTarifaRequest request) {

        PlanFunerario plan = planRepository.findByIdWithArticles(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanFunerario", id));

        if ("MUTUALISTA".equals(plan.getTipoPlan())) {
            throw new BusinessException("Use PATCH /tarifas/mutualista para el plan mutualista.");
        }
        if (esMutualistaMascotasBasico(plan)) {
            throw new BusinessException("El plan Mutualista de Mascotas Básico es predeterminado y no se puede editar.");
        }

        plan.setNombre(request.nombre());
        plan.setDescripcion(request.descripcion());
        plan.setPrecioTotal(request.precioTotal());
        plan.setMensualidad(request.mensualidad());
        plan.setDuracionMeses(request.duracionMeses());
        plan.setNumeroBeneficiarios(request.numeroBeneficiarios());
        if (request.porcentajeExtraMascota() != null) {
            plan.setPorcentajeExtraMascota(request.porcentajeExtraMascota());
        }

        if (request.articuloIds() != null) {
            plan.getArticulos().clear();
            asignarArticulos(plan, request.articuloIds());
        }

        if ("MUTUALISTA_MASCOTAS".equals(plan.getTipoPlan())) {
            plan.setAportacionDiaria(BigDecimal.ONE);
            plan.setMensualidad(plan.getPrecioTotal().divide(
                    BigDecimal.valueOf(plan.getDuracionMeses()), 2, RoundingMode.HALF_UP));
            plan.setNumeroBeneficiarios((short) 0);
            plan.setPorcentajeExtraMascota(BigDecimal.ZERO);
            plan.actualizarCondicionesMutualistaMascotas();
            plan.getArticulos().clear();
        }
        return ResponseEntity.ok(PlanFunerarioResponse.from(planRepository.save(plan)));
    }

    /** Desactiva (soft delete) un plan. */
    @DeleteMapping("/planes/{id}")
    public ResponseEntity<Void> desactivarPlan(@PathVariable Long id) {
        PlanFunerario plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanFunerario", id));
        plan.setActivo(false);
        planRepository.save(plan);
        return ResponseEntity.noContent().build();
    }

    /** Reactiva un plan desactivado. */
    @PatchMapping("/planes/{id}/activar")
    public ResponseEntity<Void> activarPlan(@PathVariable Long id) {
        PlanFunerario plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanFunerario", id));
        plan.setActivo(true);
        planRepository.save(plan);
        return ResponseEntity.noContent().build();
    }

    // ── Catálogo de artículos (para los checkboxes de mascotas) ─────────────

    /** Lista el catálogo de artículos/servicios activos. */
    @GetMapping("/articulos")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> articulos() {
        return ResponseEntity.ok(
                articuloRepository.findByActivoTrue().stream()
                        .map(a -> Map.<String, Object>of(
                                "id", a.getId(),
                                "nombre", a.getNombre(),
                                "descripcion", a.getDescripcion() != null ? a.getDescripcion() : "",
                                "categoria", a.getCategoria().getNombre()))
                        .toList());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean esMutualistaMascotasBasico(PlanFunerario plan) {
        String nombreNormalizado = Normalizer.normalize(plan.getNombre(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim();
        return "MUTUALISTA_MASCOTAS".equals(plan.getTipoPlan())
                && "Mutualista de Mascotas Basico".equalsIgnoreCase(nombreNormalizado);
    }

    private String normalizarTipo(String tipo) {
        String t = tipo.trim().toUpperCase();
        if (!List.of("MUTUALISTA", "HUMANOS", "MUTUALISTA_MASCOTAS", "MASCOTAS").contains(t)) {
            throw new BusinessException("Tipo de plan inválido: " + tipo);
        }
        return t;
    }

    private void asignarArticulos(PlanFunerario plan, List<Long> articuloIds) {
        if (articuloIds == null || articuloIds.isEmpty()) return;
        for (Long articuloId : articuloIds) {
            CatalogoArticulo articulo = articuloRepository.findById(articuloId)
                    .orElseThrow(() -> new ResourceNotFoundException("CatalogoArticulo", articuloId));

            PlanArticulo pa = new PlanArticulo();
            pa.setId(new PlanArticuloId(plan.getId(), articulo.getId()));
            pa.setPlan(plan);
            pa.setArticulo(articulo);
            pa.setCantidad((short) 1);
            plan.getArticulos().add(pa);
        }
    }
}
