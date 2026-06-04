package com.shsm.api.service;

import com.shsm.api.dto.persona.PersonaRequest;
import com.shsm.api.dto.persona.PersonaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PersonaService {
    Page<PersonaResponse> listar(String query, Pageable pageable);
    PersonaResponse obtener(Long id);
    PersonaResponse crear(PersonaRequest request);
    PersonaResponse actualizar(Long id, PersonaRequest request);
    void eliminar(Long id);
}
