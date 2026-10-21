package com.shsm.api.dto.panel;

import java.math.BigDecimal;

/** Indicadores principales del dashboard administrativo. */
public record PanelDashboardResponse(
        BigDecimal ingresosMensuales,
        long contratosActivos,
        long pagosPendientes,
        BigDecimal montoPendiente,
        long renovaciones
) {}
