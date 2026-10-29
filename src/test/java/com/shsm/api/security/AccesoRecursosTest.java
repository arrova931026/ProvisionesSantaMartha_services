package com.shsm.api.security;

import com.shsm.api.entity.Persona;
import com.shsm.api.entity.Usuario;
import com.shsm.api.repository.CobroProgramadoRepository;
import com.shsm.api.repository.ContratoRepository;
import com.shsm.api.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccesoRecursosTest {
    @Mock UsuarioRepository usuarios;
    @Mock ContratoRepository contratos;
    @Mock CobroProgramadoRepository cobros;
    @InjectMocks AccesoRecursos acceso;

    private void autenticar(String rol, long personaId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "usuario", null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
        Persona persona = new Persona();
        persona.setId(personaId);
        Usuario usuario = new Usuario();
        usuario.setPersona(persona);
        lenient().when(usuarios.findByUsername("usuario")).thenReturn(Optional.of(usuario));
    }

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    @Test
    void adminYAgentePuedenConsultarCualquierCliente() {
        for (String rol : List.of("ADMIN", "AGENTE")) {
            autenticar(rol, 3L);
            assertThat(acceso.puedeVerPersona(99L)).isTrue();
            assertThat(acceso.puedeVerContrato(99L)).isTrue();
            assertThat(acceso.puedeVerCobro(99L)).isTrue();
        }
        verifyNoInteractions(contratos, cobros);
    }

    @Test
    void clienteSoloConsultaSusPropiosRecursos() {
        autenticar("CLIENTE", 1L);
        when(contratos.existsByIdAndPersonaId(10L, 1L)).thenReturn(true);
        when(contratos.existsByIdAndPersonaId(20L, 1L)).thenReturn(false);
        when(cobros.existsByIdAndContratoPersonaId(100L, 1L)).thenReturn(true);
        when(cobros.existsByIdAndContratoPersonaId(200L, 1L)).thenReturn(false);

        assertThat(acceso.puedeVerPersona(1L)).isTrue();
        assertThat(acceso.puedeVerPersona(2L)).isFalse();
        assertThat(acceso.puedeVerContrato(10L)).isTrue();
        assertThat(acceso.puedeVerContrato(20L)).isFalse();
        assertThat(acceso.puedeVerCobro(100L)).isTrue();
        assertThat(acceso.puedeVerCobro(200L)).isFalse();
    }

    @Test
    void clientePagaSoloSusCobros() {
        autenticar("CLIENTE", 1L);
        when(cobros.existsByIdAndContratoPersonaId(100L, 1L)).thenReturn(true);
        when(cobros.existsByIdAndContratoPersonaId(200L, 1L)).thenReturn(false);
        assertThat(acceso.esCobroPropio(100L)).isTrue();
        assertThat(acceso.esCobroPropio(200L)).isFalse();
    }

    @Test
    void adminNoPuedePagarCobrosDeClientes() {
        autenticar("ADMIN", 3L);
        when(cobros.existsByIdAndContratoPersonaId(100L, 3L)).thenReturn(false);
        assertThat(acceso.esCobroPropio(100L)).isFalse();
    }

    @Test
    void sinSesionNoHayAcceso() {
        assertThat(acceso.puedeVerPersona(1L)).isFalse();
        assertThat(acceso.puedeVerContrato(1L)).isFalse();
        assertThat(acceso.esCobroPropio(1L)).isFalse();
        verifyNoInteractions(usuarios, contratos, cobros);
    }
}
