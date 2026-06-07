package com.shsm.api.service.impl;

import com.shsm.api.dto.usuario.UsuarioResponse;
import com.shsm.api.entity.Persona;
import com.shsm.api.entity.Usuario;
import com.shsm.api.exception.BusinessException;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.PersonaRepository;
import com.shsm.api.repository.TokenSesionRepository;
import com.shsm.api.repository.UsuarioRepository;
import com.shsm.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TokenSesionRepository tokenSesionRepository;
    private final PersonaRepository personaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(String q, Pageable pageable) {
        return usuarioRepository.buscarActivos(q, pageable)
                .map(UsuarioResponse::from);
    }

    @Override
    @Transactional
    public void darDeBaja(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (usuario.getDeletedAt() != null) {
            throw new BusinessException("El usuario ya fue dado de baja");
        }

        // Invalidar todos los tokens activos del usuario
        tokenSesionRepository.deleteById(id);

        OffsetDateTime ahora = OffsetDateTime.now();

        // Soft delete del usuario
        usuario.setDeletedAt(ahora);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        // Soft delete de la persona asociada para liberar correo/CURP
        Persona persona = usuario.getPersona();
        if (persona != null && persona.getDeletedAt() == null) {
            persona.setDeletedAt(ahora);
            persona.setActivo(false);
            personaRepository.save(persona);
        }
    }
}
