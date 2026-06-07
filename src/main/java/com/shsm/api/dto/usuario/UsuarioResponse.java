package com.shsm.api.dto.usuario;

import com.shsm.api.entity.Usuario;

import java.time.OffsetDateTime;

public record UsuarioResponse(
        Long id,
        String username,
        String nombreCompleto,
        String correo,
        String rol,
        Boolean activo,
        OffsetDateTime createdAt
) {
    public static UsuarioResponse from(Usuario u) {
        String nombre = u.getPersona().getNombre()
                + " " + u.getPersona().getApPaterno()
                + (u.getPersona().getApMaterno() != null ? " " + u.getPersona().getApMaterno() : "");
        return new UsuarioResponse(
                u.getId(),
                u.getUsername(),
                nombre.trim(),
                u.getPersona().getCorreo(),
                u.getRol().getClave(),
                u.getActivo(),
                u.getCreatedAt()
        );
    }
}
