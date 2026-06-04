package com.shsm.api.dto.pago;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PagoRequest(

        @NotNull(message = "El id del contrato es requerido")
        Long contratoId,

        Long cobroId,

        @NotNull(message = "El método de pago es requerido")
        Long metodoId,

        @NotNull(message = "El monto pagado es requerido")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal montoPagado,

        OffsetDateTime fechaPago,

        @Size(max = 200, message = "La referencia no puede superar 200 caracteres")
        String referenciaExterna,

        String notas
) {}
