package com.shsm.api.security;

import com.shsm.api.repository.CobroProgramadoRepository;
import com.shsm.api.repository.ContratoRepository;
import com.shsm.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Reglas de acceso por propiedad de recursos, usadas desde {@code @PreAuthorize("@acceso....")}.
 * ADMIN y AGENTE pueden consultar cualquier recurso; el resto solo los propios.
 */
@Component("acceso")
@RequiredArgsConstructor
public class AccesoRecursos {

    private static final Set<String> ROLES_STAFF = Set.of("ROLE_ADMIN", "ROLE_AGENTE");

    private final UsuarioRepository usuarioRepository;
    private final ContratoRepository contratoRepository;
    private final CobroProgramadoRepository cobroRepository;

    public boolean esStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> ROLES_STAFF.contains(a.getAuthority()));
    }

    @Transactional(readOnly = true)
    public boolean puedeVerPersona(Long personaId) {
        return esStaff() || esPersonaPropia(personaId);
    }

    @Transactional(readOnly = true)
    public boolean puedeVerContrato(Long contratoId) {
        return esStaff() || esContratoPropio(contratoId);
    }

    @Transactional(readOnly = true)
    public boolean puedeVerCobro(Long cobroId) {
        return esStaff() || esCobroPropio(cobroId);
    }

    /** Solo el titular del contrato puede iniciar/confirmar pagos de sus cobros. */
    @Transactional(readOnly = true)
    public boolean esCobroPropio(Long cobroId) {
        return cobroId != null && personaAutenticada()
                .map(pid -> cobroRepository.existsByIdAndContratoPersonaId(cobroId, pid))
                .orElse(false);
    }

    private boolean esPersonaPropia(Long personaId) {
        return personaId != null && personaAutenticada().map(personaId::equals).orElse(false);
    }

    private boolean esContratoPropio(Long contratoId) {
        return contratoId != null && personaAutenticada()
                .map(pid -> contratoRepository.existsByIdAndPersonaId(contratoId, pid))
                .orElse(false);
    }

    private Optional<Long> personaAutenticada() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return usuarioRepository.findByUsername(auth.getName())
                .map(u -> u.getPersona().getId());
    }
}
