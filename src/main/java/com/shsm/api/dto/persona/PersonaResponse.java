package com.shsm.api.dto.persona;

import com.shsm.api.entity.Persona;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PersonaResponse(
        Long id,
        String nombre,
        String apPaterno,
        String apMaterno,
        String nombreCompleto,
        LocalDate fechaNacimiento,
        String sexo,
        String curp,
        String rfc,
        String telefono,
        String telefonoAlt,
        String correo,
        String calle,
        String numeroExt,
        String numeroInt,
        String colonia,
        String municipio,
        String estado,
        String codigoPostal,
        String pais,
        Boolean activo,
        OffsetDateTime createdAt
) {
    public static PersonaResponse from(Persona p) {
        return new PersonaResponse(
                p.getId(),
                p.getNombre(),
                p.getApPaterno(),
                p.getApMaterno(),
                p.getNombre() + " " + p.getApPaterno()
                        + (p.getApMaterno() != null ? " " + p.getApMaterno() : ""),
                p.getFechaNacimiento(),
                p.getSexo(),
                p.getCurp(),
                p.getRfc(),
                p.getTelefono(),
                p.getTelefonoAlt(),
                p.getCorreo(),
                p.getCalle(),
                p.getNumeroExt(),
                p.getNumeroInt(),
                p.getColonia(),
                p.getMunicipio(),
                p.getEstado(),
                p.getCodigoPostal(),
                p.getPais(),
                p.getActivo(),
                p.getCreatedAt()
        );
    }
}
