package com.shsm.api.dto.plan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Alta/edición de un plan de previsión (humanos o mascotas). */
public record PlanTarifaRequest(
        @NotBlank(message = "El nombre del plan es requerido")
        String nombre,

        String descripcion,

        @NotNull(message = "El precio total es requerido")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        BigDecimal precioTotal,

        @NotNull(message = "La mensualidad es requerida")
        @DecimalMin(value = "0.01", message = "La mensualidad debe ser mayor a 0")
        BigDecimal mensualidad,

        @NotNull(message = "La duración en meses es requerida")
        @Min(value = 1, message = "La duración mínima es 1 mes")
        Short duracionMeses,

        @NotNull(message = "El número de beneficiarios/mascotas es requerido")
        @Min(value = 0, message = "No puede ser negativo")
        Short numeroBeneficiarios,

        /** % de aumento por mascota adicional (solo planes MASCOTAS; default 0). */
        @DecimalMin(value = "0.00", message = "No puede ser negativo")
        BigDecimal porcentajeExtraMascota,

        /** IDs de artículos del catálogo incluidos en el plan (opcional). */
        List<Long> articuloIds
) {}
