package com.shsm.api.controller;

import com.shsm.api.dto.panel.PanelDashboardResponse;
import com.shsm.api.dto.panel.PanelFinanzasResponse;
import com.shsm.api.repository.CobroProgramadoRepository;
import com.shsm.api.repository.ContratoRepository;
import com.shsm.api.repository.PagoRepository;
import com.shsm.api.repository.ServicioFunerarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

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
}
