package com.pamplona.authcore.application.service;

import com.pamplona.authcore.application.exception.InvalidImageException;
import com.pamplona.authcore.application.exception.UserNotFoundException;
import com.pamplona.authcore.application.ports.out.ImageStoragePort;
import com.pamplona.authcore.application.ports.out.PasswordHasherPort;
import com.pamplona.authcore.application.ports.out.TokenProviderPort;
import com.pamplona.authcore.application.ports.out.UserRepositoryPort;
import com.pamplona.authcore.domain.Role;
import com.pamplona.authcore.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceImageTest {

    private static final String URL = "https://res.cloudinary.com/rln6jzqz/image/upload/v1/ferreteria/usuarios/user-ana.png";

    private UserRepositoryPort users;
    private ImageStoragePort storage;
    private AuthService service;
    private User ana;

    @BeforeEach
    void setUp() {
        users = mock(UserRepositoryPort.class);
        storage = mock(ImageStoragePort.class);
        service = new AuthService(users, mock(PasswordHasherPort.class), mock(TokenProviderPort.class), storage);
        ana = new User(1L, "ana", "hash", Set.of(Role.USER), null);
    }

    @Test
    void subeLaImagenGuardaLaUrlYDevuelveElUsuarioActualizado() {
        when(users.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(storage.uploadAvatar(any(byte[].class), eq("ana"))).thenReturn(URL);
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = service.updateImage("ana", new byte[]{1, 2, 3}, "image/png");

        assertEquals(URL, updated.getImageUrl());
        verify(storage).uploadAvatar(any(byte[].class), eq("ana"));
        verify(users).save(ana);
    }

    @Test
    void rechazaUnaImagenQueSuperaLosDosMegabytes() {
        when(users.findByUsername("ana")).thenReturn(Optional.of(ana));
        byte[] grande = new byte[2 * 1024 * 1024 + 1];

        assertThrows(InvalidImageException.class, () -> service.updateImage("ana", grande, "image/png"));
        verifyNoInteractions(storage);
    }

    @Test
    void rechazaUnFormatoQueNoEsImagen() {
        when(users.findByUsername("ana")).thenReturn(Optional.of(ana));

        assertThrows(InvalidImageException.class, () -> service.updateImage("ana", new byte[]{1}, "application/pdf"));
        verify(storage, never()).uploadAvatar(any(byte[].class), anyString());
    }

    @Test
    void rechazaUnArchivoVacio() {
        when(users.findByUsername("ana")).thenReturn(Optional.of(ana));

        assertThrows(InvalidImageException.class, () -> service.updateImage("ana", new byte[0], "image/png"));
        verifyNoInteractions(storage);
    }

    @Test
    void unUsuarioInexistenteEsNoEncontrado() {
        when(users.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.updateImage("fantasma", new byte[]{1}, "image/png"));
        verifyNoInteractions(storage);
    }
}
