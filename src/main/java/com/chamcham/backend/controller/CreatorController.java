package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.creator.CreatorCreateRequest;
import com.chamcham.backend.dto.creator.CreatorResponse;
import com.chamcham.backend.dto.creator.CreatorUpdateRequest;
import com.chamcham.backend.dto.review.ReviewResponse;
import com.chamcham.backend.dto.servicepackage.ServicePackageResponse;
import com.chamcham.backend.service.CreatorService;
import com.chamcham.backend.service.ReviewService;
import com.chamcham.backend.service.ServicePackageService;
import com.chamcham.backend.util.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creators")
public class CreatorController {

    private final CreatorService creatorService;
    private final ReviewService reviewService;
    private final ServicePackageService packageService;

    public CreatorController(CreatorService creatorService, ReviewService reviewService,
                             ServicePackageService packageService) {
        this.creatorService = creatorService;
        this.reviewService = reviewService;
        this.packageService = packageService;
    }

    @PostMapping
    public ResponseEntity<CreatorResponse> create(
            @Valid @RequestBody CreatorCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creatorService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CreatorResponse>> getAll() {
        return ResponseEntity.ok(creatorService.getAll());
    }

    @GetMapping("/{creatorId}")
    public ResponseEntity<CreatorResponse> getById(@PathVariable UUID creatorId) {
        return ResponseEntity.ok(creatorService.getById(creatorId));
    }

    @GetMapping("/{creatorId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getCreatorReviews(@PathVariable UUID creatorId) {
        return ResponseEntity.ok(reviewService.getReviewsByCreator(creatorId));
    }

    @GetMapping("/{creatorId}/packages")
    public ResponseEntity<PageResponse<ServicePackageResponse>> getCreatorPackages(
            @PathVariable UUID creatorId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String dealType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(packageService.getPackages(null, null, null, null,
                creatorId, null, page, limit, "createdAt"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<CreatorResponse> getByUserId(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(creatorService.getByUserId(authUser.userId(), authUser.role(), userId));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<CreatorResponse> meProfile(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(creatorService.getByUserId(authUser.userId(), authUser.role(), authUser.userId()));
    }

    @PutMapping("/{creatorId}")
    public ResponseEntity<CreatorResponse> update(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID creatorId,
            @Valid @RequestBody CreatorUpdateRequest request
    ) {
        return ResponseEntity.ok(creatorService.update(authUser.userId(), authUser.role(), creatorId, request));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<CreatorResponse> updateMe(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody CreatorUpdateRequest request
    ) {
        return ResponseEntity.ok(creatorService.update(authUser.userId(), authUser.role(), authUser.userId(), request));
    }
}

