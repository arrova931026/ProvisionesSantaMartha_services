package com.shsm.api.dto.panel;

/** Resumen de un cliente para las tablas del panel administrativo. */
public record ClienteResumenResponse(
        Long personaId,
        String nombre,
        String correo,
        long contratosActivos,
        long beneficiariosRegistrados,
        long beneficiariosPermitidos,
        long beneficiariosDisponibles
) {
    public static ClienteResumenResponse of(Long personaId, String nombre, String correo,
                                            long contratosActivos, long beneficiariosRegistrados,
                                            long beneficiariosPermitidos) {
        return new ClienteResumenResponse(personaId, nombre, correo, contratosActivos,
                beneficiariosRegistrados, beneficiariosPermitidos,
                Math.max(0, beneficiariosPermitidos - beneficiariosRegistrados));
    }
}
