package com.shsm.api.service.impl;

import com.shsm.api.dto.pago.PagoRequest;
import com.shsm.api.dto.pago.PagoResponse;
import com.shsm.api.entity.Pago;
import com.shsm.api.entity.Recibo;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.*;
import com.shsm.api.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final ContratoRepository contratoRepository;
    private final CobroProgramadoRepository cobroRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReciboRepository reciboRepository;
    private final EstadoPagoRepository estadoPagoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> listarPorContrato(Long contratoId) {
        return pagoRepository.findByContratoIdOrderByFechaPagoDesc(contratoId)
                .stream().map(PagoResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PagoResponse> listar(Long contratoId, Pageable pageable) {
        if (contratoId != null) {
            return pagoRepository.findByContratoId(contratoId, pageable).map(PagoResponse::from);
        }
        return pagoRepository.findAll(pageable).map(PagoResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse obtener(Long id) {
        return PagoResponse.from(findById(id));
    }

    @Override
    @Transactional
    public PagoResponse registrar(PagoRequest req, String usernameRegistrador) {
        Pago pago = new Pago();

        pago.setContrato(contratoRepository.findById(req.contratoId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato", req.contratoId())));

        pago.setMetodo(metodoPagoRepository.findById(req.metodoId())
                .orElseThrow(() -> new ResourceNotFoundException("MetodoPago", req.metodoId())));

        if (req.cobroId() != null) {
            var cobro = cobroRepository.findById(req.cobroId())
                    .orElseThrow(() -> new ResourceNotFoundException("CobroProgramado", req.cobroId()));
            pago.setCobro(cobro);

            // Marcar cobro como pagado
            estadoPagoRepository.findByClave("PAGADO").ifPresent(cobro::setEstado);
            cobroRepository.save(cobro);
        }

        pago.setMontoPagado(req.montoPagado());
        pago.setFechaPago(req.fechaPago() != null ? req.fechaPago() : OffsetDateTime.now());
        pago.setReferenciaExterna(req.referenciaExterna());
        pago.setNotas(req.notas());

        usuarioRepository.findByUsername(usernameRegistrador)
                .ifPresent(pago::setRegistradoPor);

        Pago saved = pagoRepository.save(pago);
        generarRecibo(saved);
        return PagoResponse.from(saved);
    }

    private Pago findById(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));
    }

    private void generarRecibo(Pago pago) {
        Recibo recibo = new Recibo();
        recibo.setPago(pago);
        recibo.setFolioRecibo(generarFolio());
        reciboRepository.save(recibo);
    }

    private String generarFolio() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long total = reciboRepository.count() + 1;
        return String.format("REC-%s-%06d", fecha, total);
    }
}
