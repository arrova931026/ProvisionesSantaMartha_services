package com.shsm.api.dto.contrato;

import com.shsm.api.entity.CobroProgramado;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para exponer CobroProgramado con campos planos compatibles con el frontend.
 * <p>
 * Mapea:
 *   estado.clave        → estadoCobro  (resuelto con regla de negocio)
 *   fechaLimite         → fechaVencimiento
 *   fechaProgramada     → fechaProgramada (informativo)
 * </p>
 * <p>
 * Regla de estado visual (aplicada aquí, en el backend):
 *   PAGADO / CANCELADO            → sin cambio
 *   fechaLimite < hoy             → VENCIDO
 *   backend almacena VENCIDO      → VENCIDO
 *   fechaLimite ≤ hoy + 7 días   → PENDIENTE
 *   fechaLimite > hoy + 7 días   → PROGRAMADA
 * </p>
 */
public record CobroProgramadoResponse(
        Long         id,
        Long         contratoId,
        Short        numeroMensualidad,
        LocalDate    fechaProgramada,
        LocalDate    fechaVencimiento,
        BigDecimal   monto,
        String       estadoCobro,
        LocalDate    fechaPago,
        String       referenciaPago
) {
    /** Resuelve el estado visual a partir del estado almacenado y la fecha límite. */
    private static String resolveEstado(CobroProgramado c) {
        String stored = c.getEstado().getClave();
        if ("PAGADO".equals(stored) || "CANCELADO".equals(stored)) return stored;
        LocalDate venc = c.getFechaLimite();
        if (venc == null) return stored;
        LocalDate hoy = LocalDate.now();
        if (venc.isBefore(hoy) || "VENCIDO".equals(stored)) return "VENCIDO";
        if (!venc.isAfter(hoy.plusDays(10))) return "PENDIENTE";
        return "PROGRAMADA";
    }

    public static CobroProgramadoResponse from(CobroProgramado c) {
        return from(c, null);
    }

    public static CobroProgramadoResponse from(CobroProgramado c, LocalDate fechaPago) {
        return new CobroProgramadoResponse(
                c.getId(),
                c.getContrato().getId(),
                c.getNumeroMensualidad(),
                c.getFechaProgramada(),
                c.getFechaLimite(),
                c.getMonto(),
                resolveEstado(c),
                fechaPago,
                null
        );
    }
}
