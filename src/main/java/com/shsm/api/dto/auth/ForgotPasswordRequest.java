package com.shsm.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "El correo es requerido")
        @Email(message = "Correo electrónico inválido")
        String correo
) {}
