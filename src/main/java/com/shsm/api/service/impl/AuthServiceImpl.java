package com.shsm.api.service.impl;

import com.shsm.api.dto.auth.LoginRequest;
import com.shsm.api.dto.auth.LoginResponse;
import com.shsm.api.dto.auth.RefreshTokenRequest;
import com.shsm.api.entity.TokenSesion;
import com.shsm.api.entity.Usuario;
import com.shsm.api.exception.BusinessException;
import com.shsm.api.repository.TokenSesionRepository;
import com.shsm.api.repository.UsuarioRepository;
import com.shsm.api.security.JwtTokenProvider;
import com.shsm.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final TokenSesionRepository tokenSesionRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("CLIENTE");

        String accessToken = tokenProvider.generateAccessToken(userDetails.getUsername(), role);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails.getUsername());

        // Persistir refresh token para permitir revocación
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow();

        TokenSesion tokenSesion = new TokenSesion();
        tokenSesion.setUsuario(usuario);
        tokenSesion.setToken(refreshToken);
        tokenSesion.setTipo("REFRESH");
        tokenSesion.setExpiraEn(OffsetDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        tokenSesionRepository.save(tokenSesion);

        // Actualizar último acceso
        usuario.setUltimoAcceso(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        return LoginResponse.of(accessToken, refreshToken, expirationMs / 1000,
                userDetails.getUsername(), role);
    }

    @Override
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.refreshToken())) {
            throw new BusinessException("Refresh token inválido o expirado");
        }

        TokenSesion sesion = tokenSesionRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Refresh token no encontrado"));

        if (Boolean.TRUE.equals(sesion.getUsado()) || sesion.getExpiraEn().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("Refresh token ya fue utilizado o expiró");
        }

        sesion.setUsado(true);
        tokenSesionRepository.save(sesion);

        Usuario usuario = sesion.getUsuario();
        String role = usuario.getRol().getClave();
        String newAccess = tokenProvider.generateAccessToken(usuario.getUsername(), role);
        String newRefresh = tokenProvider.generateRefreshToken(usuario.getUsername());

        TokenSesion nuevoToken = new TokenSesion();
        nuevoToken.setUsuario(usuario);
        nuevoToken.setToken(newRefresh);
        nuevoToken.setTipo("REFRESH");
        nuevoToken.setExpiraEn(OffsetDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        tokenSesionRepository.save(nuevoToken);

        return LoginResponse.of(newAccess, newRefresh, expirationMs / 1000,
                usuario.getUsername(), role);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        tokenSesionRepository.findByToken(refreshToken).ifPresent(t -> {
            t.setUsado(true);
            tokenSesionRepository.save(t);
        });
    }
}
