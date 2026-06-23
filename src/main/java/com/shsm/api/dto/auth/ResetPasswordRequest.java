package com.shsm.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "El token es requerido")
        String token,

        @NotBlank(message = "La contraseña es requerida")
        @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
        String nuevaPassword
) {}
