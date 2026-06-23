package com.shsm.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "El token de Google es requerido") String idToken
) {}
