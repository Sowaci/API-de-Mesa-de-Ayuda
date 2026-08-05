package com.helpdesk.api.security;

import com.helpdesk.api.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey clave;
    private final long accessTokenMs;

    public JwtService(@Value("${helpdesk.jwt.secret}") String secreto,
            @Value("${helpdesk.jwt.access-token-ms}") long accessTokenMs) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMs = accessTokenMs;
    }

    public String generarAccessToken(Usuario usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .claim("tipo", "access")
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + accessTokenMs))
                .signWith(clave)
                .compact();
    }

    public Claims parsear(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public String getEmail(Claims claims) {
        return claims.getSubject();
    }

    public String getRol(Claims claims) {
        return claims.get("rol", String.class);
    }
}
