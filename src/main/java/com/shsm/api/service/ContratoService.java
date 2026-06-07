package com.shsm.api.service;

import com.shsm.api.dto.contrato.BeneficiarioResponse;
import com.shsm.api.dto.contrato.ContratoRequest;
import com.shsm.api.dto.contrato.ContratoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContratoService {
    Page<ContratoResponse> listar(String estadoClave, Pageable pageable);
    ContratoResponse obtener(Long id);
    List<ContratoResponse> listarPorPersona(Long personaId);
    List<BeneficiarioResponse> listarBeneficiariosDeContrato(Long contratoId);
    ContratoResponse crear(ContratoRequest request);
    ContratoResponse actualizarEstado(Long id, String estadoClave);
    void cancelar(Long id);
}
