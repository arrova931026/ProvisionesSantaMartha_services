package com.shsm.api.dto.pago;

import com.shsm.api.entity.Pago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PagoResponse(
        Long id,
        Long contratoId,
        String numeroContrato,
        Long cobroId,
        String metodoClave,
        String metodoNombre,
        BigDecimal montoPagado,
        OffsetDateTime fechaPago,
        String referenciaExterna,
        String notas,
        OffsetDateTime createdAt
) {
    public static PagoResponse from(Pago p) {
        return new PagoResponse(
                p.getId(),
                p.getContrato().getId(),
                p.getContrato().getNumeroContrato(),
                p.getCobro() != null ? p.getCobro().getId() : null,
                p.getMetodo().getClave(),
                p.getMetodo().getNombre(),
                p.getMontoPagado(),
                p.getFechaPago(),
                p.getReferenciaExterna(),
                p.getNotas(),
                p.getCreatedAt()
        );
    }
}
