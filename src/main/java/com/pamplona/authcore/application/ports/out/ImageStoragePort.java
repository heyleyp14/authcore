package com.pamplona.authcore.application.ports.out;

/**
 * Puerto de salida: almacenamiento externo de imagenes de usuario.
 * El dominio y la aplicacion no conocen Cloudinary ni ningun proveedor concreto.
 */
public interface ImageStoragePort {

    /**
     * Sube la imagen del usuario y devuelve su URL publica.
     *
     * @throws com.pamplona.authcore.application.exception.StorageUnavailableException
     *         si el almacenamiento no esta configurado o falla.
     */
    String uploadAvatar(byte[] content, String username);
}
