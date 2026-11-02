package com.pamplona.authcore.infrastructure.adapters.out.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.pamplona.authcore.application.exception.StorageUnavailableException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudinaryImageStorageAdapterTest {

    private static final String URL = "https://res.cloudinary.com/rln6jzqz/image/upload/v1/ferreteria/usuarios/user-ana.png";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void subeLaImagenConUnPublicIdEstableYDevuelveLaUrl() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(Object.class), anyMap())).thenReturn(Map.of("secure_url", URL));

        CloudinaryImageStorageAdapter adapter = new CloudinaryImageStorageAdapter(cloudinary, null);

        String url = adapter.uploadAvatar(new byte[]{1, 2, 3}, "Ana.Perez");

        assertEquals(URL, url);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(Object.class), captor.capture());
        assertEquals("ferreteria/usuarios/user-ana.perez", captor.getValue().get("public_id"));
        assertEquals(true, captor.getValue().get("overwrite"));
        assertEquals(true, captor.getValue().get("invalidate"));
        assertEquals("image", captor.getValue().get("resource_type"));
    }

    @Test
    void sinCloudinaryUrlLaSubidaFallaConServicioNoDisponible() {
        CloudinaryImageStorageAdapter adapter = new CloudinaryImageStorageAdapter("", "", "");

        StorageUnavailableException error = assertThrows(StorageUnavailableException.class,
                () -> adapter.uploadAvatar(new byte[]{1}, "ana"));

        assertTrue(error.getMessage().contains("CLOUDINARY_URL"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void unErrorDeCloudinarySeTraduceAServicioNoDisponible() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(Object.class), anyMap())).thenThrow(new IOException("sin red"));

        CloudinaryImageStorageAdapter adapter = new CloudinaryImageStorageAdapter(cloudinary, null);

        assertThrows(StorageUnavailableException.class, () -> adapter.uploadAvatar(new byte[]{1}, "ana"));
    }

    @Test
    void unArchivoVacioEsUnErrorDeValidacion() {
        CloudinaryImageStorageAdapter adapter = new CloudinaryImageStorageAdapter(mock(Cloudinary.class), null);

        assertThrows(IllegalArgumentException.class, () -> adapter.uploadAvatar(new byte[0], "ana"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void enModoUnsignedUsaElPresetSinPublicNiOverwrite() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.unsignedUpload(any(Object.class), org.mockito.ArgumentMatchers.eq("ferreteria_preset"), anyMap()))
                .thenReturn(Map.of("secure_url", URL));

        CloudinaryImageStorageAdapter adapter = new CloudinaryImageStorageAdapter(cloudinary, "ferreteria_preset");

        String url = adapter.uploadAvatar(new byte[]{1, 2, 3}, "ana");

        assertEquals(URL, url);
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).unsignedUpload(any(Object.class), org.mockito.ArgumentMatchers.eq("ferreteria_preset"), captor.capture());
        assertTrue(captor.getValue().isEmpty(), "unsigned no debe enviar public_id ni overwrite");
    }
}
