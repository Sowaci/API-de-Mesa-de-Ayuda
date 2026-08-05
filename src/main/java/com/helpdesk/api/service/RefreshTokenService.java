package com.helpdesk.api.service;

import com.helpdesk.api.entity.RefreshToken;
import com.helpdesk.api.entity.Usuario;
import com.helpdesk.api.exception.TokenInvalidoException;
import com.helpdesk.api.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long vidaUtilMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
            @Value("${helpdesk.refresh-token-ms:604800000}") long vidaUtilMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.vidaUtilMs = vidaUtilMs;
    }

    public String generarToken(Usuario usuario) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entidad = RefreshToken.builder().token(hash(token)).usuario(usuario)
                .expiraEn(LocalDateTime.now().plus(vidaUtilMs, ChronoUnit.MILLIS)).revocado(false).build();
        refreshTokenRepository.save(entidad);
        return token;
    }

    @Transactional
    public RefreshToken validar(String tokenCrudo) {
        RefreshToken entidad = refreshTokenRepository.findByToken(hash(tokenCrudo))
                .orElseThrow(() -> new TokenInvalidoException("Refresh token inexistente o invalido."));

        if (Boolean.TRUE.equals(entidad.getRevocado())) {
            throw new TokenInvalidoException("Refresh token revocado.");
        }
        if (entidad.estaExpirado()) {
            entidad.setRevocado(true);
            refreshTokenRepository.save(entidad);
            throw new TokenInvalidoException("Refresh token expirado.");
        }
        return entidad;
    }

    @Transactional
    public void revocar(String tokenCrudo) {
        refreshTokenRepository.findByToken(hash(tokenCrudo))
                .ifPresent(entidad -> {
                    entidad.setRevocado(true);
                    refreshTokenRepository.save(entidad);
                });
    }

    @Transactional
    public void revocarTodos(Usuario usuario) {
        refreshTokenRepository.findByUsuarioIdAndRevocadoFalse(usuario.getId())
                .forEach(entidad -> {
                    entidad.setRevocado(true);
                    refreshTokenRepository.save(entidad);
                });
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }
}
