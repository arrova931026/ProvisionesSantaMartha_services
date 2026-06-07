package com.shsm.api.dto.contrato;

import com.shsm.api.entity.Beneficiario;

import java.math.BigDecimal;

public record BeneficiarioResponse(
        Long id,
        Long contratoId,
        Long personaId,
        String nombreCompleto,
        String parentesco,
        BigDecimal porcentajeCobertura,
        Boolean esTitular
) {
    public static BeneficiarioResponse from(Beneficiario b) {
        String nombre = b.getPersona().getNombre() + " " + b.getPersona().getApPaterno()
                + (b.getPersona().getApMaterno() != null ? " " + b.getPersona().getApMaterno() : "");
        return new BeneficiarioResponse(
                b.getId(),
                b.getContrato().getId(),
                b.getPersona().getId(),
                nombre.trim(),
                b.getParentesco() != null ? b.getParentesco().getNombre() : null,
                b.getPorcentajeCobertura(),
                b.getEsTitular()
        );
    }
}
