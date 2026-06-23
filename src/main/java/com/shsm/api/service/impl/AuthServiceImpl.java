package com.shsm.api.service.impl;

import com.shsm.api.dto.auth.ForgotPasswordRequest;
import com.shsm.api.dto.auth.GoogleLoginRequest;
import com.shsm.api.dto.auth.LoginRequest;
import com.shsm.api.dto.auth.LoginResponse;
import com.shsm.api.dto.auth.RefreshTokenRequest;
import com.shsm.api.dto.auth.RegistroRequest;
import com.shsm.api.dto.auth.ResetPasswordRequest;
import com.shsm.api.entity.Persona;
import com.shsm.api.entity.TokenSesion;
import com.shsm.api.entity.Usuario;
import com.shsm.api.entity.catalog.Role;
import com.shsm.api.exception.BusinessException;
import com.shsm.api.repository.PersonaRepository;
import com.shsm.api.repository.RoleRepository;
import com.shsm.api.repository.TokenSesionRepository;
import com.shsm.api.repository.UsuarioRepository;
import com.shsm.api.security.JwtTokenProvider;
import com.shsm.api.service.AuthService;
import com.shsm.api.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final TokenSesionRepository tokenSesionRepository;
    private final PersonaRepository personaRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // No es un bean inyectado; se inicializa directamente para no alterar el constructor de Lombok
    private final RestClient restClient = RestClient.create();

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.google.client-id}")
    private String googleClientId;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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
                userDetails.getUsername(), role, usuario.getPersona().getId());
    }

    @Override
    @Transactional
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        Map<String, Object> tokenInfo = verifyGoogleToken(request.idToken());

        String email = (String) tokenInfo.get("email");
        String emailVerified = (String) tokenInfo.get("email_verified");
        String aud = (String) tokenInfo.get("aud");

        if (!"true".equals(emailVerified)) {
            throw new BusinessException("El correo de Google no está verificado");
        }
        if (!googleClientId.equals(aud)) {
            throw new BusinessException("Token de Google inválido: audiencia incorrecta");
        }

        Optional<Persona> personaOpt = personaRepository.findByCorreo(email);
        Persona persona;
        Usuario usuario;

        if (personaOpt.isPresent()) {
            persona = personaOpt.get();
            usuario = usuarioRepository.findByUsername(email)
                    .orElseThrow(() -> new BusinessException(
                            "Este correo ya está registrado con usuario y contraseña. Use el formulario de acceso."));
        } else {
            String givenName = (String) tokenInfo.getOrDefault("given_name", "Usuario");
            String familyName = (String) tokenInfo.getOrDefault("family_name", "Google");

            persona = new Persona();
            persona.setNombre(givenName);
            persona.setApPaterno(familyName);
            persona.setCorreo(email);
            personaRepository.save(persona);

            Role rolCliente = roleRepository.findByClave("CLIENTE")
                    .orElseThrow(() -> new BusinessException("Rol CLIENTE no encontrado"));

            usuario = new Usuario();
            usuario.setPersona(persona);
            usuario.setRol(rolCliente);
            usuario.setUsername(email);
            // Contraseña aleatoria: el usuario de Google no puede iniciar sesión con contraseña
            usuario.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuarioRepository.save(usuario);
        }

        String role = usuario.getRol().getClave();
        String accessToken = tokenProvider.generateAccessToken(usuario.getUsername(), role);
        String refreshToken = tokenProvider.generateRefreshToken(usuario.getUsername());

        TokenSesion tokenSesion = new TokenSesion();
        tokenSesion.setUsuario(usuario);
        tokenSesion.setToken(refreshToken);
        tokenSesion.setTipo("REFRESH");
        tokenSesion.setExpiraEn(OffsetDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        tokenSesionRepository.save(tokenSesion);

        usuario.setUltimoAcceso(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        return LoginResponse.of(accessToken, refreshToken, expirationMs / 1000,
                usuario.getUsername(), role, persona.getId());
    }

    private Map<String, Object> verifyGoogleToken(String idToken) {
        try {
            Map<String, Object> info = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (info == null || info.containsKey("error")) {
                throw new BusinessException("Token de Google inválido o expirado");
            }
            return info;
        } catch (RestClientException e) {
            throw new BusinessException("Token de Google inválido o expirado");
        }
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
                usuario.getUsername(), role, usuario.getPersona().getId());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        tokenSesionRepository.findByToken(refreshToken).ifPresent(t -> {
            t.setUsado(true);
            tokenSesionRepository.save(t);
        });
    }

    @Override
    @Transactional
    public void registro(RegistroRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new BusinessException("El nombre de usuario ya está en uso");
        }
        if (personaRepository.findByCurp(request.curp()).isPresent()) {
            throw new BusinessException("El CURP ya está registrado");
        }
        if (personaRepository.findByCorreo(request.correo()).isPresent()) {
            throw new BusinessException("El correo electrónico ya está registrado");
        }

        Persona persona = new Persona();
        persona.setNombre(request.nombre());
        persona.setApPaterno(request.apPaterno());
        persona.setApMaterno(request.apMaterno());
        persona.setFechaNacimiento(request.fechaNacimiento());
        persona.setSexo(request.sexo());
        persona.setCurp(request.curp().toUpperCase());
        persona.setTelefono(request.telefono());
        persona.setCorreo(request.correo());
        personaRepository.save(persona);

        Role rolCliente = roleRepository.findByClave("CLIENTE")
                .orElseThrow(() -> new BusinessException("Rol CLIENTE no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setPersona(persona);
        usuario.setRol(rolCliente);
        usuario.setUsername(request.username());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Solicitud de recuperación para correo: {}", request.correo());
        // Siempre responde 204 para no revelar si el correo existe (anti-enumeración)
        personaRepository.findByCorreo(request.correo()).ifPresentOrElse(persona -> {
            log.info("Persona encontrada con id={} para correo={}", persona.getId(), request.correo());
            usuarioRepository.findAll().stream()
                    .filter(u -> u.getPersona().getId().equals(persona.getId()))
                    .findFirst()
                    .ifPresentOrElse(usuario -> {
                        log.info("Usuario encontrado: {}", usuario.getUsername());
                        String token = UUID.randomUUID().toString();
                        TokenSesion ts = new TokenSesion();
                        ts.setUsuario(usuario);
                        ts.setToken(token);
                        ts.setTipo("RECUPERACION");
                        ts.setExpiraEn(OffsetDateTime.now().plusHours(1));
                        tokenSesionRepository.save(ts);

                        String enlace = frontendUrl + "/restablecer-contrasena?token=" + token;
                        String nombre = persona.getNombre() + " " + persona.getApPaterno();
                        emailService.enviarRecuperacionContrasena(request.correo(), nombre, enlace);
                    }, () -> log.warn("No se encontró usuario para persona id={}", persona.getId()));
        }, () -> log.warn("No se encontró persona con correo: {}", request.correo()));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        TokenSesion ts = tokenSesionRepository.findByToken(request.token())
                .orElseThrow(() -> new BusinessException("El enlace de recuperación no es válido"));

        if (!ts.getTipo().equals("RECUPERACION")) {
            throw new BusinessException("El enlace de recuperación no es válido");
        }
        if (Boolean.TRUE.equals(ts.getUsado())) {
            throw new BusinessException("Este enlace ya fue utilizado");
        }
        if (ts.getExpiraEn().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("El enlace ha expirado. Solicita uno nuevo");
        }

        ts.getUsuario().setPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        ts.getUsuario().setRequiereCambioPw(false);
        usuarioRepository.save(ts.getUsuario());

        ts.setUsado(true);
        tokenSesionRepository.save(ts);
    }
}
