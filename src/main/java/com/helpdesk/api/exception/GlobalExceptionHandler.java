package com.helpdesk.api.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<Map<String, Object>> emailYaRegistrado(EmailYaRegistradoException e) {
        return build(HttpStatus.CONFLICT, "Conflict", e.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) {
        return build(HttpStatus.NOT_FOUND, "Not Found", e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> rutaNoEncontrada(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "Not Found", "Recurso no encontrado: " + e.getResourcePath());
    }

    @ExceptionHandler({ CredencialesInvalidasException.class, TokenInvalidoException.class })
    public ResponseEntity<Map<String, Object>> noAutorizado(RuntimeException e) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> errores.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        body.put("message", "Validacion fallida");
        body.put("errores", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException e) {
        String mensaje = "Cuerpo de la peticion invalido o con valores fuera del enum permitido";
        if (e.getCause() instanceof InvalidFormatException ife
                && ife.getValue() != null
                && ife.getTargetType().isEnum()) {
            mensaje = "El valor '" + ife.getValue() + "' no es valido. Valores permitidos: "
                    + String.join(", ", enumNombres(ife.getTargetType()));
        }
        return build(HttpStatus.BAD_REQUEST, "Bad Request", mensaje);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentoInvalido(IllegalArgumentException e) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accesoDenegado(AccessDeniedException e) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "No tiene permisos para acceder a este recurso");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> errorGeneral(Exception e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Error interno del servidor: " + e.getMessage());
    }

    private String[] enumNombres(Class<?> targetType) {
        Object[] constantes = targetType.getEnumConstants();
        String[] nombres = new String[constantes.length];
        for (int i = 0; i < constantes.length; i++) {
            nombres[i] = constantes[i].toString();
        }
        return nombres;
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
