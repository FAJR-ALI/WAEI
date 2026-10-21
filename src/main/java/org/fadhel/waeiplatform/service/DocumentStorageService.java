package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final Path uploadDirectory;
    private final long maxDocumentSizeBytes;

    public DocumentStorageService(
            @Value("${app.upload.dir:uploads/organization-documents}") String uploadDirectory,
            @Value("${app.upload.max-document-size-bytes:10485760}") long maxDocumentSizeBytes) {
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        this.maxDocumentSizeBytes = maxDocumentSizeBytes;

        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the organization document directory", e);
        }
    }

    public StoredDocument store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Organization document is required");
        }
        if (file.getSize() > maxDocumentSizeBytes) {
            throw new ApiException("Organization document must not exceed "
                    + (maxDocumentSizeBytes / (1024 * 1024)) + " MB");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException("Could not read the uploaded organization document");
        }

        DocumentType documentType = detectDocumentType(bytes);
        String storedName = UUID.randomUUID() + "." + documentType.extension;
        Path target = resolveStoredName(storedName);

        try {
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new ApiException("Could not save the uploaded organization document");
        }

        return new StoredDocument(
                storedName,
                safeOriginalName(file.getOriginalFilename(), documentType.extension),
                documentType.contentType,
                bytes.length);
    }

    public Resource load(String storedName) {
        Path file = resolveStoredName(storedName);
        if (!Files.isRegularFile(file)) {
            throw new ApiException("Organization document not found");
        }
        return new PathResource(file);
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
            throw new ApiException("Organization document not found");
        }

        Path resolved = uploadDirectory.resolve(storedName).normalize();
        if (!resolved.startsWith(uploadDirectory)) {
            throw new ApiException("Invalid organization document reference");
        }
        return resolved;
    }

    private String safeOriginalName(String originalName, String fallbackExtension) {
        String name = originalName;
        if (name == null || name.isBlank()) {
            return "organization-document." + fallbackExtension;
        }

        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }

        name = name.replaceAll("[\\r\\n\\t]", "_").trim();
        try {
            Path fileName = Paths.get(name).getFileName();
            if (fileName != null) {
                name = fileName.toString();
            }
        } catch (InvalidPathException ignored) {
            name = "organization-document." + fallbackExtension;
        }

        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            return "organization-document." + fallbackExtension;
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private DocumentType detectDocumentType(byte[] bytes) {
        if (startsWith(bytes, new int[]{0xFF, 0xD8, 0xFF})) {
            return new DocumentType("image/jpeg", "jpg");
        }
        if (startsWith(bytes, new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return new DocumentType("image/png", "png");
        }
        if (containsSignature(bytes, new int[]{0x25, 0x50, 0x44, 0x46})) {
            return new DocumentType("application/pdf", "pdf");
        }
        throw new ApiException("Organization document must be a PDF, JPEG or PNG file");
    }

    private boolean startsWith(byte[] bytes, int[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean containsSignature(byte[] bytes, int[] signature) {
        int window = Math.min(bytes.length, 1024);
        for (int start = 0; start + signature.length <= window; start++) {
            boolean matches = true;
            for (int i = 0; i < signature.length; i++) {
                if ((bytes[start + i] & 0xFF) != signature[i]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    public record StoredDocument(String storedName, String originalName, String contentType, long size) {
    }

    private record DocumentType(String contentType, String extension) {
    }
}
