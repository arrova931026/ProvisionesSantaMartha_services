package com.shsm.api.dto.notificacion;

import jakarta.validation.constraints.NotNull;

public record WhatsAppPreferenceRequest(@NotNull Boolean habilitada) {}