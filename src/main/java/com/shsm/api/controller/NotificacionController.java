package com.shsm.api.controller;

import com.shsm.api.dto.notificacion.WhatsAppPreferenceRequest;
import com.shsm.api.entity.Notificacion;
import com.shsm.api.exception.ResourceNotFoundException;
import com.shsm.api.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import com.shsm.api.repository.UsuarioRepository;

@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/whatsapp-preferencia")
    @Transactional(readOnly = true)
    public ResponseEntity<java.util.Map<String, Boolean>> obtenerPreferenciaWhatsApp(
            @AuthenticationPrincipal UserDetails userDetails) {
        var usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return ResponseEntity.ok(java.util.Map.of(
                "habilitada", Boolean.TRUE.equals(usuario.getPersona().getWhatsappOptIn())));
    }

    @PutMapping("/whatsapp-preferencia")
    @Transactional
    public ResponseEntity<java.util.Map<String, Boolean>> actualizarPreferenciaWhatsApp(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WhatsAppPreferenceRequest request) {
        var usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        usuario.getPersona().setWhatsappOptIn(request.habilitada());
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(java.util.Map.of("habilitada", request.habilitada()));
    }

    @GetMapping
    public ResponseEntity<Page<Notificacion>> listar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        Long usuarioId = usuarioRepository.findByUsername(userDetails.getUsername())
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return ResponseEntity.ok(
                notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId, pageable));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<Long> contarNoLeidas(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long usuarioId = usuarioRepository.findByUsername(userDetails.getUsername())
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return ResponseEntity.ok(notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId));
    }

    @PatchMapping("/marcar-leidas")
    public ResponseEntity<Void> marcarTodasLeidas(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long usuarioId = usuarioRepository.findByUsername(userDetails.getUsername())
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        notificacionRepository.marcarTodasLeidas(usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeida(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", id));
        n.setLeida(true);
        notificacionRepository.save(n);
        return ResponseEntity.noContent().build();
    }
}
