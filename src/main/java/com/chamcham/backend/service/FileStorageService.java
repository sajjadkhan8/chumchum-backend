package com.chamcham.backend.service;

import com.chamcham.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    public static final Set<String> IMAGE_TYPES  = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    public static final Set<String> VIDEO_TYPES  = Set.of("video/mp4", "video/quicktime", "video/x-msvideo");
    public static final long MB = 1024L * 1024L;

    private final Path uploadRoot;
    private final String baseUrl;

    public FileStorageService(
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

    /**
     * Validate content-type and size, then store the file under {@code subfolder}.
     *
     * @return the public URL of the stored file
     */
    public String validateAndStore(MultipartFile file, Set<String> allowedTypes, long maxMb, String subfolder) {
        if (file.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must not be empty");
        String ct = file.getContentType();
        if (ct == null || !allowedTypes.contains(ct.toLowerCase()))
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unsupported file type: " + ct + ". Allowed: " + allowedTypes);
        if (file.getSize() > maxMb * MB)
            throw new ApiException(HttpStatus.BAD_REQUEST, "File exceeds maximum size of " + maxMb + " MB");
        return store(file, subfolder);
    }

    /**
     * Store without type validation (caller is responsible for validation).
     *
     * @return the public URL of the stored file
     */
    public String store(MultipartFile file, String subfolder) {
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains("."))
            ext = original.substring(original.lastIndexOf('.'));
        String filename = UUID.randomUUID() + ext;
        Path dir = uploadRoot.resolve(subfolder);
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file: " + e.getMessage());
        }
        return baseUrl + "/" + subfolder + "/" + filename;
    }
}

