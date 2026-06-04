package com.shsm.api.dto.contrato;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratoRequest(

        @NotNull(message = "El id de persona (titular) es requerido")
        Long personaId,

        @NotNull(message = "El id del plan es requerido")
        Long planId,

        Long sucursalId,
        Long empleadoVendedorId,

        @NotNull(message = "La fecha de inicio es requerida")
        LocalDate fechaInicio,

        @NotNull(message = "El precio contratado es requerido")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        BigDecimal precioContratado,

        @NotNull(message = "La mensualidad pactada es requerida")
        @DecimalMin(value = "0.01", message = "La mensualidad debe ser mayor a 0")
        BigDecimal mensualidadPactada,

        String notas
) {}
