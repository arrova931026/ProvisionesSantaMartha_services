package com.shsm.api.service;

import com.shsm.api.dto.usuario.UsuarioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {
    Page<UsuarioResponse> listar(String q, Pageable pageable);
    void darDeBaja(Long id);
}
