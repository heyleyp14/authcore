package com.pamplona.authcore.application.exception;

/**
 * El almacenamiento externo de archivos no esta configurado o no respondio.
 */
public class StorageUnavailableException extends RuntimeException {

    public StorageUnavailableException(String message) {
        super(message);
    }

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
