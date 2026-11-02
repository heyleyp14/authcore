package com.pamplona.authcore.infrastructure.adapters.out.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.pamplona.authcore.application.exception.StorageUnavailableException;
import com.pamplona.authcore.application.ports.out.ImageStoragePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Adaptador de salida: sube las imagenes de usuario a Cloudinary.
 *
 * Dos modos, en este orden:
 * 1. Firmado con {@code CLOUDINARY_URL} (recomendado: requiere permiso de subida en la API key).
 * 2. Unsigned con {@code CLOUDINARY_CLOUD_NAME} + {@code CLOUDINARY_UPLOAD_PRESET} (no usa el secret).
 *
 * Sin ninguno de los dos el servicio arranca igual y la subida responde 503.
 */
@Component
public class CloudinaryImageStorageAdapter implements ImageStoragePort {

    private static final String FOLDER = "ferreteria/usuarios";

    private final Cloudinary cloudinary;
    /** null cuando se usa el modo firmado. */
    private final String unsignedPreset;

    @Autowired
    public CloudinaryImageStorageAdapter(@Value("${authcore.cloudinary.url:}") String cloudinaryUrl,
                                         @Value("${authcore.cloudinary.cloud-name:}") String cloudName,
                                         @Value("${authcore.cloudinary.upload-preset:}") String uploadPreset) {
        this(toCloudinary(cloudinaryUrl, cloudName, uploadPreset), blank(uploadPreset) ? null : uploadPreset.trim());
    }

    CloudinaryImageStorageAdapter(Cloudinary cloudinary, String unsignedPreset) {
        this.cloudinary = cloudinary;
        this.unsignedPreset = unsignedPreset;
    }

    private static Cloudinary toCloudinary(String cloudinaryUrl, String cloudName, String uploadPreset) {
        if (!blank(cloudinaryUrl)) {
            return new Cloudinary(cloudinaryUrl.trim());
        }
        if (!blank(cloudName) && !blank(uploadPreset)) {
            return new Cloudinary(ObjectUtils.asMap("cloud_name", cloudName.trim(), "secure", true));
        }
        return null;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public String uploadAvatar(byte[] content, String username) {
        if (cloudinary == null) {
            throw new StorageUnavailableException(
                    "Cloudinary no esta configurado; defina CLOUDINARY_URL o CLOUDINARY_CLOUD_NAME + CLOUDINARY_UPLOAD_PRESET");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("El archivo de imagen esta vacio");
        }

        try {
            Map<?, ?> result = unsignedPreset != null
                    ? cloudinary.uploader().unsignedUpload(content, unsignedPreset, ObjectUtils.emptyMap())
                    : cloudinary.uploader().upload(content, signedOptions(username));
            Object url = result.get("secure_url");
            if (url == null) {
                throw new StorageUnavailableException("Cloudinary no devolvio la URL de la imagen");
            }
            return url.toString();
        } catch (IOException e) {
            throw new StorageUnavailableException("No se pudo subir la imagen a Cloudinary", e);
        }
    }

    private static Map<String, Object> signedOptions(String username) {
        return ObjectUtils.asMap(
                "public_id", FOLDER + "/user-" + slug(username),
                "overwrite", true,
                "invalidate", true,
                "resource_type", "image"
        );
    }

    /** Un public_id estable por usuario: en modo firmado, la nueva imagen reemplaza a la anterior. */
    private static String slug(String username) {
        String value = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        value = value.replaceAll("[^a-z0-9._-]", "-");
        return value.isBlank() ? "usuario" : value;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
