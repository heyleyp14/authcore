package com.pamplona.authcore.infrastructure.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username es obligatorio") String username,
        @NotBlank(message = "password es obligatorio")
        @Size(min = 6, message = "password debe tener al menos 6 caracteres") String password,
        @Size(max = 512, message = "imageUrl no puede superar los 512 caracteres")
        @Pattern(regexp = "^https?://.+$", message = "imageUrl debe ser una URL http(s) válida")
        String imageUrl
) {}
