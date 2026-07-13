package com.shsm.api.dto.contrato;

import com.shsm.api.entity.CobroProgramado;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para exponer CobroProgramado con campos planos compatibles con el frontend.
 * <p>
 * Mapea:
 *   estado.clave        → estadoCobro
 *   fechaLimite         → fechaVencimiento
 *   fechaProgramada     → fechaProgramada (informativo)
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
                c.getEstado().getClave(),
                fechaPago,
                null
        );
    }
}
