package com.helpdesk.api.controller;

import com.helpdesk.api.dto.response.AuthResponse;
import com.helpdesk.api.dto.request.LoginRequest;
import com.helpdesk.api.dto.request.RefreshRequest;
import com.helpdesk.api.dto.response.RefreshResponse;
import com.helpdesk.api.dto.request.RegistroRequest;
import com.helpdesk.api.entity.Usuario;
import com.helpdesk.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.renovar(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal Usuario usuario,
            @RequestBody(required = false) RefreshRequest request) {
        authService.logout(usuario.getEmail(), request != null ? request.refreshToken() : null);
        return ResponseEntity.ok(Map.of("message", "Sesion cerrada, refresh token revocado."));
    }
}
