package com.pamplona.authcore.application.service;

import com.pamplona.authcore.application.exception.InvalidCredentialsException;
import com.pamplona.authcore.application.exception.InvalidImageException;
import com.pamplona.authcore.application.exception.UserAlreadyExistsException;
import com.pamplona.authcore.application.exception.UserNotFoundException;
import com.pamplona.authcore.application.ports.in.AssignRoleUseCase;
import com.pamplona.authcore.application.ports.in.CurrentUserUseCase;
import com.pamplona.authcore.application.ports.in.ListUsersUseCase;
import com.pamplona.authcore.application.ports.in.LoginUseCase;
import com.pamplona.authcore.application.ports.in.RegisterUserUseCase;
import com.pamplona.authcore.application.ports.in.UpdateUserImageUseCase;
import com.pamplona.authcore.application.ports.out.ImageStoragePort;
import com.pamplona.authcore.application.ports.out.Pagina;
import com.pamplona.authcore.application.ports.out.Paginacion;
import com.pamplona.authcore.application.ports.out.PasswordHasherPort;
import com.pamplona.authcore.application.ports.out.TokenProviderPort;
import com.pamplona.authcore.application.ports.out.UserRepositoryPort;
import com.pamplona.authcore.domain.Role;
import com.pamplona.authcore.domain.User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Servicio de aplicacion: implementa los casos de uso (puertos de entrada)
 * usando SOLO los puertos de salida. No importa nada de "infrastructure".
 */
@Service
public class AuthService implements RegisterUserUseCase, LoginUseCase, AssignRoleUseCase, ListUsersUseCase,
        UpdateUserImageUseCase, CurrentUserUseCase {

    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenProviderPort tokenProvider;
    private final ImageStoragePort imageStorage;

    public AuthService(UserRepositoryPort userRepository,
                        PasswordHasherPort passwordHasher,
                        TokenProviderPort tokenProvider,
                        ImageStoragePort imageStorage) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.imageStorage = imageStorage;
    }

    @Override
    public User register(String username, String rawPassword, String imageUrl) {
        userRepository.findByUsername(username).ifPresent(u -> {
            throw new UserAlreadyExistsException(username);
        });
        Set<Role> defaultRoles = new HashSet<>(Set.of(Role.USER));
        User user = new User(null, username, passwordHasher.hash(rawPassword), defaultRoles,
                normalizeImageUrl(imageUrl));
        return userRepository.save(user);
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrl.trim();
    }

    @Override
    public String login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokenProvider.generateToken(user);
    }

    @Override
    public void assignRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.addRole(role);
        userRepository.save(user);
    }

    @Override
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Override
    public Pagina<User> listUsers(Paginacion paginacion) {
        return userRepository.findAll(paginacion);
    }

    @Override
    public User currentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    @Override
    public User updateImage(String username, byte[] content, String contentType) {
        User user = currentUser(username);
        validateImage(content, contentType);
        String imageUrl = imageStorage.uploadAvatar(content, user.getUsername());
        user.setImageUrl(imageUrl);
        return userRepository.save(user);
    }

    private void validateImage(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new InvalidImageException("El archivo de imagen esta vacio");
        }
        if (content.length > MAX_IMAGE_BYTES) {
            throw new InvalidImageException("La imagen no puede superar los 2 MB");
        }
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidImageException("Formato de imagen no permitido; use PNG, JPG o WebP");
        }
    }
}
