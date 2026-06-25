package com.zingzing.backend.storage;

import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.dto.media.StoredMedia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
@ConditionalOnProperty(name = "app.storage.driver", havingValue = "local", matchIfMissing = true)
public class LocalStorageStrategy implements StorageStrategy {

    private final Path uploadRoot;
    private final String baseUrl;

    public LocalStorageStrategy(
            @Value("${app.uploads.dir:./uploads}") String uploadDir,
            @Value("${app.uploads.base-url:http://localhost:8080/uploads}") String baseUrl) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory: " + uploadDir, e);
        }
    }

    @Override
    public StoredMedia store(MultipartFile file, String filename, String subfolder, String resourceType) {
        Path dir = safeResolve(subfolder);
        try {
            Files.createDirectories(dir);
            Path destination = dir.resolve(filename).normalize();
            if (!destination.startsWith(uploadRoot)) throw new IOException("Invalid destination");
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }
        String url = baseUrl + "/" + subfolder + "/" + filename;
        return new StoredMedia(url, url, url, subfolder + "/" + filename, null,
                resourceType, extension(filename), file.getSize(), null, null, null);
    }

    @Override
    public String protectedPath(String filename, String subfolder) {
        return "/api/v1/files/" + subfolder + "/" + filename;
    }

    @Override
    public Path load(String relativePath) {
        Path file = safeResolve(relativePath);
        if (!Files.isRegularFile(file)) throw new ApiException(HttpStatus.NOT_FOUND, "File not found");
        return file;
    }

    private Path safeResolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || relativePath.contains("..") || relativePath.startsWith("/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
        Path resolved = uploadRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(uploadRoot)) throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
        return resolved;
    }

    private String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : null;
    }
}
