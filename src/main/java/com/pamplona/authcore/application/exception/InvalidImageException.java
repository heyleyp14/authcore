package com.pamplona.authcore.application.exception;

/** La imagen enviada no es valida (vacia, demasiado grande o de un formato no permitido). */
public class InvalidImageException extends RuntimeException {

    public InvalidImageException(String message) {
        super(message);
    }

    public InvalidImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
