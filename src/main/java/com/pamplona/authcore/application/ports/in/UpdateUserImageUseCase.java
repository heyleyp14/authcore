package com.pamplona.authcore.application.ports.in;

import com.pamplona.authcore.domain.User;

/**
 * Puerto de entrada: cambiar la imagen de un usuario autenticado.
 */
public interface UpdateUserImageUseCase {

    /**
     * Valida, sube y guarda la nueva imagen. Devuelve el usuario actualizado.
     */
    User updateImage(String username, byte[] content, String contentType);
}
