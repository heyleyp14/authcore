package com.pamplona.authcore.application.ports.in;

import com.pamplona.authcore.domain.User;

/**
 * Puerto de entrada: registrar un nuevo usuario.
 */
public interface RegisterUserUseCase {
    User register(String username, String rawPassword, String imageUrl);
}
