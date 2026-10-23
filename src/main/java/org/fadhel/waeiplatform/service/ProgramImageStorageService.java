package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class ProgramImageStorageService {

    private final Path uploadDirectory;
    private final long maxImageSizeBytes;

    public ProgramImageStorageService(
            @Value("${app.program-image.dir:uploads/program-images}") String uploadDirectory,
            @Value("${app.program-image.max-size-bytes:5242880}") long maxImageSizeBytes) {
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        this.maxImageSizeBytes = maxImageSizeBytes;

        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the program image directory", e);
        }
    }

    public StoredImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Program image is required");
        }
        if (file.getSize() > maxImageSizeBytes) {
            throw new ApiException("Program image must not exceed "
                    + (maxImageSizeBytes / (1024 * 1024)) + " MB");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException("Could not read the uploaded program image");
        }

        ImageType imageType = detectImageType(bytes);
        String storedName = UUID.randomUUID() + "." + imageType.extension;
        Path target = resolveStoredName(storedName);

        try {
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new ApiException("Could not save the uploaded program image");
        }

        return new StoredImage(storedName, imageType.contentType, bytes.length);
    }

    public Resource load(String storedName) {
        Path file = resolveStoredName(storedName);
        if (!Files.isRegularFile(file)) {
            throw new ApiException("Program image not found");
        }
        return new FileSystemResource(file);
    }

    public void deleteQuietly(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveStoredName(storedName));
        } catch (IOException | ApiException ignored) {
        }
    }

    private Path resolveStoredName(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            throw new ApiException("Program image not found");
        }

        Path resolved = uploadDirectory.resolve(storedName).normalize();
        if (!resolved.startsWith(uploadDirectory)) {
            throw new ApiException("Invalid program image reference");
        }
        return resolved;
    }

    private ImageType detectImageType(byte[] bytes) {
        if (matches(bytes, 0, new int[]{0xFF, 0xD8, 0xFF})) {
            return new ImageType("image/jpeg", "jpg");
        }
        if (matches(bytes, 0, new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return new ImageType("image/png", "png");
        }
        if (matches(bytes, 0, new int[]{0x52, 0x49, 0x46, 0x46})
                && matches(bytes, 8, new int[]{0x57, 0x45, 0x42, 0x50})) {
            return new ImageType("image/webp", "webp");
        }
        throw new ApiException("Program image must be a JPEG, PNG or WebP file");
    }

    private boolean matches(byte[] bytes, int offset, int[] signature) {
        if (bytes.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[offset + i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    public record StoredImage(String storedName, String contentType, long size) {
    }

    private record ImageType(String contentType, String extension) {
    }
}
