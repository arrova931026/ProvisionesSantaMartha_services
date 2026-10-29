package com.shsm.api.dto.contrato;

import com.shsm.api.entity.Contrato;
import com.shsm.api.entity.MascotaContrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ContratoResponse(
        Long id,
        String numeroContrato,
        Long personaId,
        String titularNombre,
        Long planId,
        String planNombre,
        Short planNumeroBeneficiarios,
        Long sucursalId,
        String estadoClave,
        String estadoNombre,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        BigDecimal precioContratado,
        BigDecimal mensualidadPactada,
        String frecuenciaPago,
        String notas,
        Boolean activo,
        OffsetDateTime createdAt,
        String tipoPlan,
        MascotaContrato mascota,
        BigDecimal aportacionDiaria,
        String condiciones
) {
    public static ContratoResponse from(Contrato c) {
        String titular = c.getPersona().getNombre() + " " + c.getPersona().getApPaterno()
                + (c.getPersona().getApMaterno() != null ? " " + c.getPersona().getApMaterno() : "");
        return new ContratoResponse(
                c.getId(),
                c.getNumeroContrato(),
                c.getPersona().getId(),
                titular,
                c.getPlan().getId(),
                c.getPlan().getNombre(),
                c.getPlan().getNumeroBeneficiarios(),
                c.getSucursal() != null ? c.getSucursal().getId() : null,
                c.getEstado().getClave(),
                c.getEstado().getNombre(),
                c.getFechaInicio(),
                c.getFechaVencimiento(),
                c.getPrecioContratado(),
                c.getMensualidadPactada(),
                c.getFrecuenciaPago(),
                c.getNotas(),
                c.getActivo(),
                c.getCreatedAt(),
                c.getPlan().getTipoPlan(),
                c.getMascota(),
                c.getAportacionDiaria(),
                c.getCondiciones()
        );
    }
}
