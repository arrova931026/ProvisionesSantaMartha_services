package com.shsm.api.service.impl;

import com.shsm.api.dto.persona.PersonaRequest;
import com.shsm.api.dto.persona.PersonaResponse;
import com.shsm.api.entity.Persona;
import com.shsm.api.exception.BusinessException;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.PersonaRepository;
import com.shsm.api.service.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PersonaServiceImpl implements PersonaService {

    private final PersonaRepository personaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PersonaResponse> listar(String query, Pageable pageable) {
        if (StringUtils.hasText(query)) {
            return personaRepository.buscar(query, pageable).map(PersonaResponse::from);
        }
        return personaRepository.findAll(pageable).map(PersonaResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResponse obtener(Long id) {
        return PersonaResponse.from(findById(id));
    }

    @Override
    @Transactional
    public PersonaResponse crear(PersonaRequest request) {
        if (StringUtils.hasText(request.correo()) &&
                personaRepository.findByCorreo(request.correo()).isPresent()) {
            throw new BusinessException("El correo ya está registrado: " + request.correo());
        }
        if (StringUtils.hasText(request.curp()) &&
                personaRepository.findByCurp(request.curp()).isPresent()) {
            throw new BusinessException("La CURP ya está registrada: " + request.curp());
        }
        Persona persona = mapToEntity(new Persona(), request);
        return PersonaResponse.from(personaRepository.save(persona));
    }

    @Override
    @Transactional
    public PersonaResponse actualizar(Long id, PersonaRequest request) {
        Persona persona = findById(id);

        if (StringUtils.hasText(request.correo()) &&
                !request.correo().equals(persona.getCorreo()) &&
                personaRepository.findByCorreo(request.correo()).isPresent()) {
            throw new BusinessException("El correo ya está registrado: " + request.correo());
        }
        mapToEntity(persona, request);
        return PersonaResponse.from(personaRepository.save(persona));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Persona persona = findById(id);
        persona.setActivo(false);
        persona.setDeletedAt(OffsetDateTime.now());
        personaRepository.save(persona);
    }

    private Persona findById(Long id) {
        return personaRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", id));
    }

    private Persona mapToEntity(Persona p, PersonaRequest r) {
        p.setNombre(r.nombre());
        p.setApPaterno(r.apPaterno());
        p.setApMaterno(r.apMaterno());
        p.setFechaNacimiento(r.fechaNacimiento());
        p.setSexo(r.sexo());
        p.setCurp(r.curp());
        p.setRfc(r.rfc());
        p.setTelefono(r.telefono());
        p.setTelefonoAlt(r.telefonoAlt());
        p.setCorreo(r.correo());
        p.setCalle(r.calle());
        p.setNumeroExt(r.numeroExt());
        p.setNumeroInt(r.numeroInt());
        p.setColonia(r.colonia());
        p.setMunicipio(r.municipio());
        p.setEstado(r.estado());
        p.setCodigoPostal(r.codigoPostal());
        if (StringUtils.hasText(r.pais())) p.setPais(r.pais());
        return p;
    }
}
