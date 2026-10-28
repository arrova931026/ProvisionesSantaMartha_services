package com.shsm.api.controller;

import com.shsm.api.dto.contrato.ContratoResponse;
import com.shsm.api.dto.panel.ClienteResumenResponse;
import com.shsm.api.dto.panel.PanelDashboardResponse;
import com.shsm.api.dto.panel.PanelFinanzasResponse;
import com.shsm.api.entity.Contrato;
import com.shsm.api.entity.Persona;
import com.shsm.api.repository.BeneficiarioRepository;
import com.shsm.api.repository.CobroProgramadoRepository;
import com.shsm.api.repository.ContratoRepository;
import com.shsm.api.repository.PagoRepository;
import com.shsm.api.repository.PersonaRepository;
import com.shsm.api.repository.ServicioFunerarioRepository;
import com.shsm.api.service.ContratoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Estadísticas del Panel Administrativo (ADMIN y AGENTE).
 */
@RestController
@RequestMapping("/panel")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
public class PanelController {

    private final PagoRepository pagoRepository;
    private final ContratoRepository contratoRepository;
    private final CobroProgramadoRepository cobroRepository;
    private final ServicioFunerarioRepository servicioRepository;
    private final PersonaRepository personaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final ContratoService contratoService;

    /** Indicadores del dashboard: ingresos del mes, contratos activos, pagos pendientes y renovaciones. */
    @GetMapping("/dashboard")
    public ResponseEntity<PanelDashboardResponse> dashboard() {
        YearMonth mesActual = YearMonth.now();
        OffsetDateTime inicioMes = mesActual.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime finMes    = mesActual.atEndOfMonth().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        BigDecimal ingresosMensuales = pagoRepository.sumMontoBetween(inicioMes, finMes);
        long contratosActivos        = contratoRepository.countByEstadoClave("VIGENTE");
        long pagosPendientes         = cobroRepository.countPendientes();
        BigDecimal montoPendiente    = cobroRepository.sumMontoPendiente();
        long renovaciones            = contratoRepository.countRenovacionesProximas(
                LocalDate.now(), LocalDate.now().plusDays(30));

        return ResponseEntity.ok(new PanelDashboardResponse(
                ingresosMensuales, contratosActivos, pagosPendientes, montoPendiente, renovaciones));
    }

    /** Indicadores de finanzas: morosidad, gestión de servicios y resumen de estados. */
    @GetMapping("/finanzas")
    public ResponseEntity<PanelFinanzasResponse> finanzas() {
        LocalDate hoy = LocalDate.now();
        YearMonth mesActual = YearMonth.now();

        long sociosMorosos      = contratoRepository.countMorosos(hoy);
        BigDecimal montoVencido = cobroRepository.sumMontoVencido(hoy);

        long serviciosEnProceso = servicioRepository.countEnProceso();
        long serviciosMes       = servicioRepository.countByMes(mesActual.getYear(), mesActual.getMonthValue());

        OffsetDateTime inicioDia = hoy.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime finDia    = hoy.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
        long pagosHoy            = pagoRepository.countBetween(inicioDia, finDia);
        BigDecimal ingresosHoy   = pagoRepository.sumMontoBetween(inicioDia, finDia);

        long vigentes    = contratoRepository.countByEstadoClave("VIGENTE");
        long suspendidos = contratoRepository.countByEstadoClave("SUSPENDIDO");
        long vencidos    = contratoRepository.countByEstadoClave("VENCIDO");
        long cancelados  = contratoRepository.countByEstadoClave("CANCELADO");

        return ResponseEntity.ok(new PanelFinanzasResponse(
                sociosMorosos, montoVencido, serviciosEnProceso, serviciosMes,
                pagosHoy, ingresosHoy, vigentes, suspendidos, vencidos, cancelados));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Operaciones: resumen de clientes para las tablas del panel
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Lista clientes (personas activas) con conteo de contratos activos y
     * disponibilidad de beneficiarios. Búsqueda opcional por nombre/correo.
     */
    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteResumenResponse>> clientes(
            @RequestParam(required = false) String q) {

        List<Persona> personas = (q != null && !q.isBlank())
                ? personaRepository.buscar(q, org.springframework.data.domain.Pageable.unpaged()).getContent()
                : personaRepository.findAll().stream()
                        .filter(p -> p.getDeletedAt() == null && Boolean.TRUE.equals(p.getActivo()))
                        .toList();

        List<ClienteResumenResponse> resumen = personas.stream().map(p -> {
            List<Contrato> vigentes = contratoRepository.findVigentesByPersonaId(p.getId());
            long registrados = 0;
            long permitidos = 0;
            for (Contrato c : vigentes) {
                registrados += beneficiarioRepository.countByContratoIdAndActivoTrue(c.getId());
                permitidos  += c.getPlan().getNumeroBeneficiarios();
            }
            String nombreCompleto = (p.getNombre() + " " + p.getApPaterno()
                    + (p.getApMaterno() != null ? " " + p.getApMaterno() : "")).trim();
            return ClienteResumenResponse.of(
                    p.getId(), nombreCompleto, p.getCorreo(),
                    vigentes.size(), registrados, permitidos);
        }).toList();

        return ResponseEntity.ok(resumen);
    }

    /** Contratos vigentes de un cliente (para el modal de eliminación). */
    @GetMapping("/clientes/{personaId}/contratos")
    public ResponseEntity<List<ContratoResponse>> contratosDeCliente(@PathVariable Long personaId) {
        return ResponseEntity.ok(contratoService.listarPorPersona(personaId));
    }

    /** Elimina (cancela) un contrato. Solo ADMIN. */
    @DeleteMapping("/contratos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarContrato(@PathVariable Long id) {
        contratoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
