package com.helpdesk.api.service;

import com.helpdesk.api.dto.AuthResponse;
import com.helpdesk.api.dto.LoginRequest;
import com.helpdesk.api.dto.RefreshResponse;
import com.helpdesk.api.dto.RegistroRequest;
import com.helpdesk.api.entity.RefreshToken;
import com.helpdesk.api.entity.Usuario;
import com.helpdesk.api.enums.Rol;
import com.helpdesk.api.exception.CredencialesInvalidasException;
import com.helpdesk.api.exception.EmailYaRegistradoException;
import com.helpdesk.api.exception.TokenInvalidoException;
import com.helpdesk.api.repository.UsuarioRepository;
import com.helpdesk.api.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.email().toLowerCase())) {
            throw new EmailYaRegistradoException("El email " + request.email() + " ya esta registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .rol(Rol.USUARIO)
                .build();
        usuarioRepository.save(usuario);

        return emitirTokens(usuario);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().toLowerCase(), request.password()));
        } catch (BadCredentialsException e) {
            throw new CredencialesInvalidasException("Email o password incorrectos");
        }

        Usuario usuario = usuarioRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new CredencialesInvalidasException("Email o password incorrectos"));

        return emitirTokens(usuario);
    }

    /**
     * Renovacion del access token usando el refresh token. Aplica rotacion:
     * el refresh token usado se revoca y se emite uno nuevo.
     */
    @Transactional
    public RefreshResponse renovar(String refreshTokenCrudo) {
        RefreshToken entidad = refreshTokenService.validar(refreshTokenCrudo);

        Usuario usuario = entidad.getUsuario();
        String nuevoAccess = jwtService.generarAccessToken(usuario);

        // Rotacion: revocar el refresh token usado y emitir uno nuevo
        refreshTokenService.revocar(refreshTokenCrudo);
        String nuevoRefresh = refreshTokenService.generarToken(usuario);

        return new RefreshResponse(nuevoAccess, nuevoRefresh);
    }

    @Transactional
    public void logout(String email, String refreshTokenCrudo) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new TokenInvalidoException("Usuario no encontrado"));

        if (refreshTokenCrudo != null && !refreshTokenCrudo.isBlank()) {
            refreshTokenService.revocar(refreshTokenCrudo);
        } else {
            refreshTokenService.revocarTodos(usuario);
        }
    }

    private AuthResponse emitirTokens(Usuario usuario) {
        String access = jwtService.generarAccessToken(usuario);
        String refresh = refreshTokenService.generarToken(usuario);
        return new AuthResponse(access, refresh, usuario.getEmail(), usuario.getNombre(), usuario.getRol().name());
    }
}
