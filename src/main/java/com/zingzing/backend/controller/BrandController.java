package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.brand.BrandUpdateRequest;
import com.zingzing.backend.dto.brand.BrandResponse;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.BrandVerificationDocument;
import com.zingzing.backend.entity.BrandVerificationEvent;
import com.zingzing.backend.entity.enums.BrandPlanTier;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.BrandRepository;
import com.zingzing.backend.repository.BrandVerificationDocumentRepository;
import com.zingzing.backend.repository.BrandVerificationEventRepository;
import com.zingzing.backend.util.BrandVerificationStatuses;
import com.zingzing.backend.service.BrandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandService brandService;
    private final BrandRepository brandRepository;
    private final BrandVerificationDocumentRepository verificationDocumentRepository;
    private final BrandVerificationEventRepository verificationEventRepository;

    public BrandController(BrandService brandService,
                           BrandRepository brandRepository,
                           BrandVerificationDocumentRepository verificationDocumentRepository,
                           BrandVerificationEventRepository verificationEventRepository) {
        this.brandService = brandService;
        this.brandRepository = brandRepository;
        this.verificationDocumentRepository = verificationDocumentRepository;
        this.verificationEventRepository = verificationEventRepository;
    }

    @GetMapping("/me/verification-events")
    public ResponseEntity<Map<String, Object>> verificationEvents(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        requireBrand(authUser);
        List<Map<String, Object>> events = verificationEventRepository
                .findByBrandIdOrderByCreatedAtDesc(authUser.userId())
                .stream()
                .map(this::toVerificationEventMap)
                .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", events));
    }

    public record PlanSelectRequest(@NotNull @NotBlank String planTier) {}
    public record VerificationDocumentRequest(
            @NotBlank @Size(max = 40) String type,
            @NotBlank @Size(max = 600) String fileUrl,
            @NotBlank @Size(max = 255) String fileName
    ) {}

    @GetMapping
    public ResponseEntity<List<BrandResponse>> getAll() {
        return ResponseEntity.ok(brandService.getAll());
    }

    @GetMapping("/{brandId}")
    public ResponseEntity<BrandResponse> getById(@PathVariable UUID brandId) {
        return ResponseEntity.ok(brandService.getById(brandId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<BrandResponse> getByUserId(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(brandService.getByUserId(authUser.userId(), authUser.role(), userId));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<BrandResponse> meProfile(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(brandService.getByUserId(authUser.userId(), authUser.role(), authUser.userId()));
    }

    @PutMapping("/{brandId}")
    public ResponseEntity<BrandResponse> update(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID brandId,
            @Valid @RequestBody BrandUpdateRequest request
    ) {
        return ResponseEntity.ok(brandService.update(authUser.userId(), authUser.role(), brandId, request));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<BrandResponse> updateMe(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandUpdateRequest request
    ) {
        return ResponseEntity.ok(brandService.update(authUser.userId(), authUser.role(), authUser.userId(), request));
    }

    @PatchMapping("/me/plan")
    @Transactional
    public ResponseEntity<Map<String, Object>> selectPlan(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody PlanSelectRequest request
    ) {
        if (!authUser.role().isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can select a plan");
        }
        BrandPlanTier tier;
        try {
            tier = BrandPlanTier.valueOf(request.planTier().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid plan tier. Valid values: STARTER, GROWTH, ENTERPRISE");
        }
        Brand brand = brandRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));
        brand.setPlanTier(tier);
        brandRepository.save(brand);
        return ResponseEntity.ok(Map.of("success", true, "planTier", tier.name()));
    }

    @GetMapping("/me/verification-documents")
    public ResponseEntity<Map<String, Object>> verificationDocuments(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        requireBrand(authUser);
        List<Map<String, Object>> docs = verificationDocumentRepository
                .findByBrandIdOrderByUploadedAtDesc(authUser.userId())
                .stream()
                .map(this::toVerificationDocumentMap)
                .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", docs));
    }

    @PostMapping("/me/verification-documents")
    @Transactional
    public ResponseEntity<Map<String, Object>> submitVerificationDocument(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody VerificationDocumentRequest request
    ) {
        requireBrand(authUser);
        Brand brand = brandRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));
        BrandVerificationDocument saved = verificationDocumentRepository.save(BrandVerificationDocument.builder()
                .brand(brand)
                .type(request.type().trim())
                .fileUrl(request.fileUrl().trim())
                .fileName(request.fileName().trim())
                .status("PENDING")
                .build());
        if (brand.getBusinessVerificationStatus() == null || brand.getBusinessVerificationStatus().isBlank()) {
            brand.setBusinessVerificationStatus(BrandVerificationStatuses.PENDING);
            brandRepository.save(brand);
        }
        verificationEventRepository.save(BrandVerificationEvent.builder()
                .brand(brand)
                .document(saved)
                .actor(brand)
                .eventType("DOCUMENT_UPLOADED")
                .details(request.type().trim() + ": " + request.fileName().trim())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", toVerificationDocumentMap(saved)));
    }

    @PostMapping("/me/verification/submit")
    @Transactional
    public ResponseEntity<Map<String, Object>> submitVerificationForReview(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        requireBrand(authUser);
        Brand brand = brandRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));
        if (verificationDocumentRepository.findByBrandIdOrderByUploadedAtDesc(brand.getId()).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload at least one verification document before submitting");
        }
        brand.setBusinessVerificationStatus(BrandVerificationStatuses.UNDER_REVIEW);
        brandRepository.save(brand);
        verificationEventRepository.save(BrandVerificationEvent.builder()
                .brand(brand)
                .actor(brand)
                .eventType("SUBMITTED_FOR_REVIEW")
                .details("Brand submitted verification documents for review")
                .build());
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("success", true)));
    }

    private void requireBrand(AuthenticatedUser authUser) {
        if (authUser == null || !authUser.role().isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can use this endpoint");
        }
    }

    private Map<String, Object> toVerificationDocumentMap(BrandVerificationDocument doc) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", doc.getId());
        row.put("type", doc.getType());
        row.put("fileName", doc.getFileName());
        row.put("fileUrl", doc.getFileUrl());
        row.put("status", doc.getStatus().toLowerCase());
        row.put("rejectionReason", doc.getRejectionReason());
        row.put("uploadedAt", doc.getUploadedAt());
        return row;
    }

    private Map<String, Object> toVerificationEventMap(BrandVerificationEvent event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", event.getId());
        row.put("eventType", event.getEventType());
        row.put("details", event.getDetails());
        row.put("documentId", event.getDocument() == null ? null : event.getDocument().getId());
        row.put("createdAt", event.getCreatedAt());
        return row;
    }
}
