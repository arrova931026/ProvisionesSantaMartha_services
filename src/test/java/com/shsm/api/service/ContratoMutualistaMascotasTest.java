package com.shsm.api.service;

import com.shsm.api.dto.contrato.BeneficiarioRequest;
import com.shsm.api.dto.contrato.ContratoRequest;
import com.shsm.api.entity.CobroProgramado;
import com.shsm.api.entity.Contrato;
import com.shsm.api.entity.MascotaContrato;
import com.shsm.api.entity.PlanFunerario;
import com.shsm.api.entity.catalog.EstadoContrato;
import com.shsm.api.entity.catalog.EstadoPago;
import com.shsm.api.exception.BusinessException;
import com.shsm.api.repository.*;
import com.shsm.api.service.impl.ContratoServiceImpl;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContratoMutualistaMascotasTest {
    @Mock ContratoRepository contratos;
    @Mock PersonaRepository personas;
    @Mock PlanFunerarioRepository planes;
    @Mock SucursalRepository sucursales;
    @Mock EmpleadoRepository empleados;
    @Mock EstadoContratoRepository estadosContrato;
    @Mock EstadoPagoRepository estadosPago;
    @Mock CobroProgramadoRepository cobros;
    @Mock BeneficiarioRepository beneficiarios;
    @Mock ParentescoRepository parentescos;
    @InjectMocks ContratoServiceImpl service;

    private PlanFunerario plan;

    @BeforeEach
    void prepararPlan() {
        plan = MutualistaMascotasFixtures.plan();
    }

    private ContratoRequest request(MascotaContrato mascota, LocalDate inicio,
                                    BigDecimal precio, BigDecimal mensualidad, String frecuencia) {
        return new ContratoRequest(1L, 2L, null, null, inicio, precio, mensualidad,
                frecuencia, "Contrato de prueba", mascota);
    }

    private ContratoRequest request(MascotaContrato mascota) {
        return request(mascota, LocalDate.of(2026, 10, 29), BigDecimal.ONE, BigDecimal.ONE, null);
    }

    private void prepararConsulta() {
        when(personas.findById(1L)).thenReturn(Optional.of(MutualistaMascotasFixtures.persona()));
        when(planes.findById(2L)).thenReturn(Optional.of(plan));
    }

    private void prepararCreacion() {
        prepararConsulta();
        EstadoContrato vigente = new EstadoContrato();
        vigente.setClave("VIGENTE");
        vigente.setNombre("Vigente");
        when(estadosContrato.findByClave("VIGENTE")).thenReturn(Optional.of(vigente));
        when(estadosPago.findByClave("PENDIENTE")).thenReturn(Optional.of(new EstadoPago()));
        when(contratos.save(any(Contrato.class))).thenAnswer(invocation -> {
            Contrato contrato = invocation.getArgument(0);
            contrato.setId(10L);
            return contrato;
        });
    }

    @Test
    void exigeMascotaAntesDeGuardarContratoOCobros() {
        prepararConsulta();

        assertThatThrownBy(() -> service.crear(request(null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("mascota");

        verify(contratos, never()).save(any());
        verifyNoInteractions(cobros, beneficiarios);
    }

    @ParameterizedTest(name = "rechaza {0} con valor [{1}]")
    @CsvSource(value = {
            "nombre,NULL", "nombre,''", "nombre,'   '",
            "especie,NULL", "especie,''", "especie,'   '",
            "raza,NULL", "raza,''", "raza,'   '",
            "color,NULL", "color,''", "color,'   '"
    }, nullValues = "NULL", ignoreLeadingAndTrailingWhitespace = false)
    void rechazaCamposIncompletos(String campo, String valor) {
        prepararConsulta();
        MascotaContrato mascota = MutualistaMascotasFixtures.mascota();
        switch (campo) {
            case "nombre" -> mascota.setNombre(valor);
            case "especie" -> mascota.setEspecie(valor);
            case "raza" -> mascota.setRaza(valor);
            case "color" -> mascota.setColorCaracteristicas(valor);
            default -> throw new IllegalArgumentException(campo);
        }

        assertThatThrownBy(() -> service.crear(request(mascota)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Complete nombre");

        verify(contratos, never()).save(any());
        verifyNoInteractions(cobros, beneficiarios);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @ValueSource(strings = {"0", "-0.001", "-10"})
    void rechazaPesoAusenteCeroONegativo(String peso) {
        prepararConsulta();
        MascotaContrato mascota = MutualistaMascotasFixtures.mascota();
        mascota.setPeso(peso == null ? null : new BigDecimal(peso));

        assertThatThrownBy(() -> service.crear(request(mascota)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("peso");

        verify(contratos, never()).save(any());
        verifyNoInteractions(cobros, beneficiarios);
    }

    @ParameterizedTest
    @CsvSource({"2026-10-29,2027-10-29", "2024-02-29,2025-02-28", "2023-03-01,2024-03-01"})
    void guardaMascotaVigenciaYDoceCobrosQueSumanElTotal(String fechaInicio, String fechaFin) {
        prepararCreacion();
        LocalDate inicio = LocalDate.parse(fechaInicio);
        MascotaContrato mascota = MutualistaMascotasFixtures.mascota();

        var response = service.crear(request(mascota, inicio,
                new BigDecimal("9999.99"), new BigDecimal("999.99"), null));

        ArgumentCaptor<Contrato> contratoCaptor = ArgumentCaptor.forClass(Contrato.class);
        verify(contratos).save(contratoCaptor.capture());
        Contrato guardado = contratoCaptor.getValue();
        assertThat(guardado.getMascota()).usingRecursiveComparison().isEqualTo(mascota);
        assertThat(guardado.getFechaInicio()).isEqualTo(inicio);
        assertThat(guardado.getFechaVencimiento()).isEqualTo(LocalDate.parse(fechaFin));
        assertThat(guardado.getPrecioContratado()).isEqualByComparingTo("365.00");
        assertThat(guardado.getMensualidadPactada()).isEqualByComparingTo("30.42");
        assertThat(guardado.getAportacionDiaria()).isEqualByComparingTo("1.00");
        assertThat(guardado.getCondiciones()).isEqualTo(plan.getCondiciones());
        assertThat(response.mascota()).usingRecursiveComparison().isEqualTo(mascota);
        assertThat(response.fechaVencimiento()).isEqualTo(LocalDate.parse(fechaFin));
        assertThat(response.precioContratado()).isEqualByComparingTo("365.00");
        assertThat(response.mensualidadPactada()).isEqualByComparingTo("30.42");
        assertThat(response.planNumeroBeneficiarios()).isZero();
        assertThat(response.frecuenciaPago()).isEqualTo("MENSUAL");

        ArgumentCaptor<CobroProgramado> cobroCaptor = ArgumentCaptor.forClass(CobroProgramado.class);
        verify(cobros, times(12)).save(cobroCaptor.capture());
        var programados = cobroCaptor.getAllValues();
        BigDecimal total = BigDecimal.ZERO;
        for (CobroProgramado cobro : programados) {
            total = total.add(cobro.getMonto());
        }
        assertThat(total).isEqualByComparingTo("365.00");
        for (int indice = 0; indice < 12; indice++) {
            CobroProgramado cobro = programados.get(indice);
            assertThat(cobro.getContrato()).isSameAs(guardado);
            assertThat(cobro.getNumeroMensualidad()).isEqualTo((short) (indice + 1));
            assertThat(cobro.getFechaProgramada()).isEqualTo(inicio.plusMonths(indice));
            assertThat(cobro.getFechaLimite()).isEqualTo(inicio.plusMonths(indice).plusDays(10));
            assertThat(cobro.getMonto()).isEqualByComparingTo(indice == 11 ? "30.38" : "30.42");
            assertThat(cobro.getEstado()).isNotNull();
        }
        verifyNoInteractions(beneficiarios);
    }

    @Test
    void servicioAceptaImportesNulosYUsaLosDelBackend() {
        prepararCreacion();

        var response = service.crear(request(MutualistaMascotasFixtures.mascota(),
                LocalDate.of(2026, 10, 29), null, null, "MENSUAL"));

        assertThat(response.precioContratado()).isEqualByComparingTo("365.00");
        assertThat(response.mensualidadPactada()).isEqualByComparingTo("30.42");
        verify(cobros, times(12)).save(any());
    }

    @Test
    void validacionDelRequestDebePermitirImportesCalculadosPorBackend() {
        var request = request(MutualistaMascotasFixtures.mascota(),
                LocalDate.of(2026, 10, 29), null, null, "MENSUAL");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request))
                    .as("El cliente no debe estar obligado a enviar precio ni mensualidad del plan")
                    .isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SEMANAL", "QUINCENAL", "ANUAL"})
    void rechazaFrecuenciasNoMensuales(String frecuencia) {
        prepararConsulta();

        assertThatThrownBy(() -> service.crear(request(MutualistaMascotasFixtures.mascota(),
                LocalDate.of(2026, 10, 29), BigDecimal.ONE, BigDecimal.ONE, frecuencia)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("solo permite pago mensual");

        verify(contratos, never()).save(any());
        verifyNoInteractions(cobros);
    }

    @Test
    void noPermiteAgregarBeneficiariosAunqueElPlanTengaUnLimiteIncorrecto() {
        Contrato contrato = MutualistaMascotasFixtures.contrato();
        contrato.getPlan().setNumeroBeneficiarios((short) 5);
        when(contratos.findById(10L)).thenReturn(Optional.of(contrato));
        BeneficiarioRequest request = new BeneficiarioRequest("Luis", "Perez", null,
                "2010-01-01", "7821234567", "luis@example.test", 1L, new BigDecimal("100"));

        assertThatThrownBy(() -> service.agregarBeneficiario(10L, request))
                .isInstanceOf(BusinessException.class).hasMessageContaining("no permite beneficiarios");

        verifyNoInteractions(beneficiarios, personas, parentescos);
    }
}