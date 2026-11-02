package com.pamplona.authcore.infrastructure.adapters.in.rest.dto;

import java.util.List;

/** Página de usuarios para la administración (solo ADMIN). */
public record UsuariosR(long total, int pagina, int tamano, int totalPaginas, List<UserResponse> usuarios) { }
