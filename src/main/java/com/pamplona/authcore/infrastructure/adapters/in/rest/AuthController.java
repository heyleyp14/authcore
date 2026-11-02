package com.pamplona.authcore.infrastructure.adapters.in.rest;

import com.pamplona.authcore.application.ports.in.AssignRoleUseCase;
import com.pamplona.authcore.application.ports.in.ListUsersUseCase;
import com.pamplona.authcore.application.ports.in.LoginUseCase;
import com.pamplona.authcore.application.ports.in.RegisterUserUseCase;
import com.pamplona.authcore.application.ports.out.Pagina;
import com.pamplona.authcore.application.ports.out.Paginacion;
import com.pamplona.authcore.domain.User;
import com.pamplona.authcore.infrastructure.adapters.in.rest.dto.AssignRoleRequest;
import com.pamplona.authcore.infrastructure.adapters.in.rest.dto.LoginRequest;
import com.pamplona.authcore.infrastructure.adapters.in.rest.dto.LoginResponse;
import com.pamplona.authcore.infrastructure.adapters.in.rest.dto.RegisterRequest;
import com.pamplona.authcore.infrastructure.adapters.in.rest.dto.RegisterResponse;
import com.pamplona.authcore.infrastructure.adapters.in.rest.dto.UserResponse;
import com.pamplona.authcore.infrastructure.adapters.in.rest.dto.UsuariosR;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST. Este es el UNICO punto de contacto que
 * dominio-service (y cualquier cliente) tiene con authcore-service.
 * Contrato: ver README.md
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final AssignRoleUseCase assignRoleUseCase;
    private final ListUsersUseCase listUsersUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                           LoginUseCase loginUseCase,
                           AssignRoleUseCase assignRoleUseCase,
                           ListUsersUseCase listUsersUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.assignRoleUseCase = assignRoleUseCase;
        this.listUsersUseCase = listUsersUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        User created = registerUserUseCase.register(request.username(), request.password(), request.imageUrl());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(created.getId(), created.getUsername(), created.getImageUrl()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = loginUseCase.login(request.username(), request.password());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignRole(@PathVariable Long id, @Valid @RequestBody AssignRoleRequest request) {
        assignRoleUseCase.assignRole(id, request.role());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuariosR listUsers(@RequestParam(defaultValue = "1") int pagina,
                               @RequestParam(defaultValue = "20") int tamano) {
        Pagina<User> p = listUsersUseCase.listUsers(Paginacion.de(pagina, tamano));
        return new UsuariosR(p.total(), p.pagina(), p.tamano(), p.totalPaginas(),
                p.contenido().stream().map(UserResponse::from).toList());
    }
}
