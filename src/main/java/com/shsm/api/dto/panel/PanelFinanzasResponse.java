package com.shsm.api.dto.panel;

import java.math.BigDecimal;

/** Indicadores de la pestaña Finanzas del panel administrativo. */
public record PanelFinanzasResponse(
        long sociosMorosos,
        BigDecimal montoVencido,
        long serviciosEnProceso,
        long serviciosMes,
        long pagosHoy,
        BigDecimal ingresosHoy,
        long contratosVigentes,
        long contratosSuspendidos,
        long contratosVencidos,
        long contratosCancelados
) {}
