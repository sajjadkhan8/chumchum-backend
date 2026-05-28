package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.chamcham.backend.service.FileStorageService.IMAGE_TYPES;
import static com.chamcham.backend.service.FileStorageService.MB;
import static com.chamcham.backend.service.FileStorageService.VIDEO_TYPES;
import com.chamcham.backend.exception.ApiException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private final FileStorageService fileStorageService;

    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> avatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ok(fileStorageService.validateAndStore(file, IMAGE_TYPES, 5, "avatars"));
    }

    @PostMapping(value = "/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> coverImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ok(fileStorageService.validateAndStore(file, IMAGE_TYPES, 10, "covers"));
    }

    @PostMapping(value = "/content-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> contentPreview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String platform,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        Set<String> allowed = new HashSet<>(IMAGE_TYPES);
        allowed.addAll(VIDEO_TYPES);
        return ok(fileStorageService.validateAndStore(file, allowed, 100, "previews"));
    }

    @PostMapping(value = "/package-thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> packageThumbnail(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ok(fileStorageService.validateAndStore(file, IMAGE_TYPES, 5, "packages"));
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
        return ok(fileStorageService.store(file, "deliverables"));
    }

    @PostMapping(value = "/brand-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> brandLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ok(fileStorageService.validateAndStore(file, IMAGE_TYPES, 5, "brands"));
    }

    private ResponseEntity<Map<String, Object>> ok(String url) {
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("url", url)));
    }
}
