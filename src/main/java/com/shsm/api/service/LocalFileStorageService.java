package com.shsm.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class LocalFileStorageService {

    private final Path profilePicturesDir;
    private final Path docsDir;

    public LocalFileStorageService(
            @Value("${app.profile-pictures-dir:profile_pictures}") String profilePicturesDir,
            @Value("${app.docs-dir:docs}") String docsDir) {
        this.profilePicturesDir = Path.of(profilePicturesDir).toAbsolutePath().normalize();
        this.docsDir = Path.of(docsDir).toAbsolutePath().normalize();
    }

    public String storeProfilePicture(InputStream content, String filename) throws IOException {
        store(profilePicturesDir, filename, content);
        return "/profile_pictures/" + filename;
    }

    public String storeDocument(InputStream content, String relativePath) throws IOException {
        store(docsDir, relativePath, content);
        return "/docs/" + relativePath.replace('\\', '/');
    }

    public void deleteProfilePicture(String filename) {
        delete(profilePicturesDir, filename);
    }

    public void deleteDocument(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return;

        String relativePath = storedPath;
        int docsMarker = relativePath.lastIndexOf("/docs/");
        if (docsMarker >= 0) {
            relativePath = relativePath.substring(docsMarker + "/docs/".length());
        } else if (relativePath.startsWith("docs/")) {
            relativePath = relativePath.substring("docs/".length());
        }
        delete(docsDir, relativePath);
    }

    private void store(Path root, String relativePath, InputStream content) throws IOException {
        Path destination = resolve(root, relativePath);
        Files.createDirectories(destination.getParent());
        try (InputStream input = content) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void delete(Path root, String relativePath) {
        try {
            Files.deleteIfExists(resolve(root, relativePath));
        } catch (IOException | IllegalArgumentException e) {
            log.warn("No se pudo eliminar archivo local {}: {}", relativePath, e.getMessage());
        }
    }

    private Path resolve(Path root, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("La ruta del archivo está vacía");
        }
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("La ruta del archivo está fuera del almacenamiento permitido");
        }
        return target;
    }
}