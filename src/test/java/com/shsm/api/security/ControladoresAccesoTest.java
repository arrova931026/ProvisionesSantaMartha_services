package com.shsm.api.security;

import com.shsm.api.controller.CobroController;
import com.shsm.api.controller.ContratoController;
import com.shsm.api.controller.PagoController;
import com.shsm.api.dto.contrato.ContratoRequest;
import com.shsm.api.repository.CobroProgramadoRepository;
import com.shsm.api.repository.PagoRepository;
import com.shsm.api.service.ContratoService;
import com.shsm.api.service.PagoService;
import com.shsm.api.service.PdfContratoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Verifica que los @PreAuthorize de los controladores delegan en {@link AccesoRecursos}. */
@SpringJUnitConfig(ControladoresAccesoTest.Config.class)
@TestPropertySource(properties = {
        "mercadopago.access-token=test", "mercadopago.back-url-base=http://localhost"})
class ControladoresAccesoTest {

    @Configuration
    @EnableMethodSecurity
    @Import({CobroController.class, ContratoController.class, PagoController.class})
    static class Config {
        @Bean AccesoRecursos acceso() { return Mockito.mock(AccesoRecursos.class); }
        @Bean CobroProgramadoRepository cobroRepository() { return Mockito.mock(CobroProgramadoRepository.class); }
        @Bean PagoRepository pagoRepository() { return Mockito.mock(PagoRepository.class); }
        @Bean ContratoService contratoService() { return Mockito.mock(ContratoService.class); }
        @Bean PdfContratoService pdfContratoService() { return Mockito.mock(PdfContratoService.class); }
        @Bean PagoService pagoService() { return Mockito.mock(PagoService.class); }
    }

    @Autowired AccesoRecursos acceso;
    @Autowired CobroController cobros;
    @Autowired ContratoController contratos;
    @Autowired PagoController pagos;

    private void cliente() {
        reset(acceso);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "cliente", null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));
    }

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    @Test
    void cobrosDeContratoAjenoSonRechazados() {
        cliente();
        when(acceso.puedeVerContrato(5L)).thenReturn(false);
        assertThatThrownBy(() -> cobros.listarPorContrato(5L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> cobros.pendientesPorContrato(5L)).isInstanceOf(AccessDeniedException.class);

        when(acceso.puedeVerContrato(6L)).thenReturn(true);
        assertThatCode(() -> cobros.listarPorContrato(6L)).doesNotThrowAnyException();
    }

    @Test
    void cobroIndividualAjenoEsRechazado() {
        cliente();
        when(acceso.puedeVerCobro(9L)).thenReturn(false);
        assertThatThrownBy(() -> cobros.obtener(9L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void contratosDeOtraPersonaSonRechazados() {
        cliente();
        when(acceso.puedeVerPersona(2L)).thenReturn(false);
        when(acceso.puedeVerContrato(7L)).thenReturn(false);
        assertThatThrownBy(() -> contratos.listarPorPersona(2L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> contratos.obtener(7L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> contratos.listarBeneficiarios(7L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> contratos.descargarPdf(7L)).isInstanceOf(AccessDeniedException.class);

        when(acceso.puedeVerPersona(1L)).thenReturn(true);
        assertThatCode(() -> contratos.listarPorPersona(1L)).doesNotThrowAnyException();
    }

    @Test
    void clienteNoPuedeCrearContratoParaOtraPersona() {
        cliente();
        ContratoRequest request = mock(ContratoRequest.class);
        when(request.personaId()).thenReturn(2L);
        when(acceso.puedeVerPersona(2L)).thenReturn(false);
        assertThatThrownBy(() -> contratos.crear(request)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pagoDeCobroAjenoEsRechazado() {
        cliente();
        when(acceso.esCobroPropio(3L)).thenReturn(false);
        assertThatThrownBy(() -> pagos.crearPreferenciaMP(3L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> pagos.confirmarPagoMP(3L, "1")).isInstanceOf(AccessDeniedException.class);
        verify(acceso, never()).puedeVerCobro(any());
    }
}
