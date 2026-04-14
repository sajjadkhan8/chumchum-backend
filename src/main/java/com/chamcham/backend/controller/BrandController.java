package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.brand.BrandCreateRequest;
import com.chamcham.backend.dto.brand.BrandResponse;
import com.chamcham.backend.dto.brand.BrandUpdateRequest;
import com.chamcham.backend.service.BrandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @PostMapping
    public ResponseEntity<BrandResponse> create(
            @Valid @RequestBody BrandCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(brandService.create(request));
    }

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

    @PutMapping("/{brandId}")
    public ResponseEntity<BrandResponse> update(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID brandId,
            @Valid @RequestBody BrandUpdateRequest request
    ) {
        return ResponseEntity.ok(brandService.update(authUser.userId(), authUser.role(), brandId, request));
    }
}

