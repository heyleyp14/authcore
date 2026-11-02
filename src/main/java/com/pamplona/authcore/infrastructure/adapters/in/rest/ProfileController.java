package com.pamplona.authcore.infrastructure.adapters.in.rest;

import com.pamplona.authcore.application.exception.InvalidImageException;
import com.pamplona.authcore.application.ports.in.CurrentUserUseCase;
import com.pamplona.authcore.application.ports.in.UpdateUserImageUseCase;
import com.pamplona.authcore.domain.User;
import com.pamplona.authcore.infrastructure.adapters.in.rest.dto.ImageResponse;
import com.pamplona.authcore.infrastructure.adapters.in.rest.dto.UserResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Adaptador de entrada REST para el perfil del usuario autenticado.
 * La identidad sale del JWT, nunca del cuerpo de la peticion.
 */
@RestController
@RequestMapping("/api/auth")
public class ProfileController {

    private final CurrentUserUseCase currentUserUseCase;
    private final UpdateUserImageUseCase updateUserImageUseCase;

    public ProfileController(CurrentUserUseCase currentUserUseCase,
                             UpdateUserImageUseCase updateUserImageUseCase) {
        this.currentUserUseCase = currentUserUseCase;
        this.updateUserImageUseCase = updateUserImageUseCase;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from(currentUserUseCase.currentUser(authentication.getName()));
    }

    @PostMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageResponse uploadImage(Authentication authentication,
                                     @RequestParam("archivo") MultipartFile archivo) {
        byte[] content;
        try {
            content = archivo.getBytes();
        } catch (IOException ex) {
            throw new InvalidImageException("No se pudo leer el archivo de imagen", ex);
        }
        User updated = updateUserImageUseCase.updateImage(
                authentication.getName(), content, archivo.getContentType());
        return new ImageResponse(updated.getImageUrl());
    }
}
