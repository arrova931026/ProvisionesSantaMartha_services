package com.shsm.api.dto.contrato;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record BeneficiarioRequest(

        @NotBlank(message = "El nombre del beneficiario es requerido")
        String nombre,

        @NotBlank(message = "El apellido paterno del beneficiario es requerido")
        String apPaterno,

        String apMaterno,

        String fechaNacimiento,   // ISO date yyyy-MM-dd, opcional

        @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
        String telefono,

        @Email(message = "Correo inválido")
        String correo,

        @NotNull(message = "El parentesco es requerido")
        Long parentescoId,

        @NotNull(message = "El porcentaje de cobertura es requerido")
        @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor a 0")
        @DecimalMax(value = "100.00", message = "El porcentaje no puede superar 100")
        BigDecimal porcentajeCobertura
) {}
