package com.shsm.api.service.impl;

import com.shsm.api.dto.contrato.ContratoRequest;
import com.shsm.api.dto.contrato.ContratoResponse;
import com.shsm.api.entity.CobroProgramado;
import com.shsm.api.entity.Contrato;
import com.shsm.api.entity.catalog.EstadoContrato;
import com.shsm.api.entity.catalog.EstadoPago;
import com.shsm.api.exception.BusinessException;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.*;
import com.shsm.api.service.ContratoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratoServiceImpl implements ContratoService {

    private final ContratoRepository contratoRepository;
    private final PersonaRepository personaRepository;
    private final PlanFunerarioRepository planRepository;
    private final SucursalRepository sucursalRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EstadoContratoRepository estadoContratoRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final CobroProgramadoRepository cobroProgramadoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ContratoResponse> listar(String estadoClave, Pageable pageable) {
        if (StringUtils.hasText(estadoClave)) {
            return contratoRepository.findByEstadoClave(estadoClave, pageable)
                    .map(ContratoResponse::from);
        }
        return contratoRepository.findAll(pageable).map(ContratoResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoResponse obtener(Long id) {
        return ContratoResponse.from(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoResponse> listarPorPersona(Long personaId) {
        return contratoRepository.findByPersonaIdAndActivoTrue(personaId)
                .stream().map(ContratoResponse::from).toList();
    }

    @Override
    @Transactional
    public ContratoResponse crear(ContratoRequest req) {
        Contrato contrato = new Contrato();

        contrato.setPersona(personaRepository.findById(req.personaId())
                .orElseThrow(() -> new ResourceNotFoundException("Persona", req.personaId())));

        var plan = planRepository.findById(req.planId())
                .orElseThrow(() -> new ResourceNotFoundException("PlanFunerario", req.planId()));
        contrato.setPlan(plan);

        if (req.sucursalId() != null) {
            contrato.setSucursal(sucursalRepository.findById(req.sucursalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal", req.sucursalId())));
        }

        if (req.empleadoVendedorId() != null) {
            contrato.setEmpleadoVendedor(empleadoRepository.findById(req.empleadoVendedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado", req.empleadoVendedorId())));
        }

        EstadoContrato estadoVigente = estadoContratoRepository.findByClave("VIGENTE")
                .orElseThrow(() -> new BusinessException("Estado VIGENTE no encontrado en catálogo"));
        contrato.setEstado(estadoVigente);

        contrato.setFechaInicio(req.fechaInicio());
        contrato.setFechaVencimiento(req.fechaInicio()
                .plusMonths(plan.getDuracionMeses()));
        contrato.setPrecioContratado(req.precioContratado());
        contrato.setMensualidadPactada(req.mensualidadPactada());
        contrato.setNotas(req.notas());
        contrato.setNumeroContrato(generarNumeroContrato());

        Contrato saved = contratoRepository.save(contrato);
        generarCobros(saved, plan.getDuracionMeses());
        return ContratoResponse.from(saved);
    }

    @Override
    @Transactional
    public ContratoResponse actualizarEstado(Long id, String estadoClave) {
        Contrato contrato = findById(id);
        EstadoContrato estado = estadoContratoRepository.findByClave(estadoClave)
                .orElseThrow(() -> new BusinessException("Estado inválido: " + estadoClave));
        contrato.setEstado(estado);
        return ContratoResponse.from(contratoRepository.save(contrato));
    }

    @Override
    @Transactional
    public void cancelar(Long id) {
        actualizarEstado(id, "CANCELADO");
        Contrato contrato = findById(id);
        contrato.setActivo(false);
        contrato.setDeletedAt(OffsetDateTime.now());
        contratoRepository.save(contrato);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Contrato findById(Long id) {
        return contratoRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato", id));
    }

    private String generarNumeroContrato() {
        String anio = String.valueOf(LocalDate.now().getYear());
        long total = contratoRepository.count() + 1;
        return String.format("SHSM-%s-%05d", anio, total);
    }

    private void generarCobros(Contrato contrato, int meses) {
        EstadoPago pendiente = estadoPagoRepository.findByClave("PENDIENTE")
                .orElseThrow(() -> new BusinessException("Estado PENDIENTE no encontrado en catálogo"));

        for (int i = 1; i <= meses; i++) {
            CobroProgramado cobro = new CobroProgramado();
            cobro.setContrato(contrato);
            cobro.setNumeroMensualidad((short) i);
            cobro.setFechaProgramada(contrato.getFechaInicio().plusMonths(i - 1));
            cobro.setFechaLimite(contrato.getFechaInicio().plusMonths(i - 1).plusDays(10));
            cobro.setMonto(contrato.getMensualidadPactada());
            cobro.setEstado(pendiente);
            cobroProgramadoRepository.save(cobro);
        }
    }
}
