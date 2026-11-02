package com.pamplona.authcore.application.ports.in;

import com.pamplona.authcore.domain.User;

/**
 * Puerto de entrada: consultar el perfil del usuario autenticado.
 */
public interface CurrentUserUseCase {
    User currentUser(String username);
}
