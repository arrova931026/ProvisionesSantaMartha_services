package com.shsm.api.service;

import com.shsm.api.dto.pago.PagoRequest;
import com.shsm.api.dto.pago.PagoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PagoService {
    List<PagoResponse> listarPorContrato(Long contratoId);
    Page<PagoResponse> listar(Long contratoId, Pageable pageable);
    PagoResponse obtener(Long id);
    PagoResponse registrar(PagoRequest request, String usernameRegistrador);
}
