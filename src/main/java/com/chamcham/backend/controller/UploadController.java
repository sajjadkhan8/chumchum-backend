package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4", "video/quicktime", "video/x-msvideo");
    private static final long MB = 1024L * 1024L;

    private final Path uploadRoot;
    private final String baseUrl;

    public UploadController(
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

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> avatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        validate(file, IMAGE_TYPES, 5);
        return ok(store(file, "avatars"));
    }

    @PostMapping(value = "/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> coverImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        validate(file, IMAGE_TYPES, 10);
        return ok(store(file, "covers"));
    }

    @PostMapping(value = "/content-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> contentPreview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String platform,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        Set<String> allowed = new HashSet<>(IMAGE_TYPES);
        allowed.addAll(VIDEO_TYPES);
        validate(file, allowed, 100);
        return ok(store(file, "previews"));
    }

    @PostMapping(value = "/package-thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> packageThumbnail(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        validate(file, IMAGE_TYPES, 5);
        return ok(store(file, "packages"));
    }

    @PostMapping(value = "/deliverable", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> deliverable(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String deliverableId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        if (file.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must not be empty");
        if (file.getSize() > 500 * MB)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deliverable files must be under 500 MB");
        return ok(store(file, "deliverables"));
    }

    @PostMapping(value = "/brand-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> brandLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        validate(file, IMAGE_TYPES, 5);
        return ok(store(file, "brands"));
    }

    // ---- helpers ----

    private void validate(MultipartFile file, Set<String> allowedTypes, long maxMb) {
        if (file.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must not be empty");
        String ct = file.getContentType();
        if (ct == null || !allowedTypes.contains(ct.toLowerCase()))
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unsupported file type: " + ct + ". Allowed: " + allowedTypes);
        if (file.getSize() > maxMb * MB)
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "File exceeds maximum size of " + maxMb + " MB");
    }

    private String store(MultipartFile file, String subfolder) {
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

    private ResponseEntity<Map<String, Object>> ok(String url) {
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("url", url)));
    }
}

