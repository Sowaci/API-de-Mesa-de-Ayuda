package com.helpdesk.api.exception;

public class EmailYaRegistradoException extends RuntimeException {
    public EmailYaRegistradoException(String mensaje) {
        super(mensaje);
    }
}
