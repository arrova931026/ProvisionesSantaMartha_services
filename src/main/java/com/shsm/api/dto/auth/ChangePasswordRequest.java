package com.shsm.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String passwordActual,
        @NotBlank @Size(min = 8) String nuevaPassword
) {}
