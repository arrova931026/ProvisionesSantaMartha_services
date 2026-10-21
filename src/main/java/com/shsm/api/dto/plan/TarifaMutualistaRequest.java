package com.shsm.api.dto.plan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Actualización de la aportación anual del plan mutualista. */
public record TarifaMutualistaRequest(
        @NotNull(message = "La aportación anual es requerida")
        @DecimalMin(value = "0.01", message = "La aportación debe ser mayor a 0")
        BigDecimal aportacionAnual
) {}
