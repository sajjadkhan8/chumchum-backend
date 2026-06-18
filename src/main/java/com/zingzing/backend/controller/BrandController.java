package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.brand.BrandUpdateRequest;
import com.zingzing.backend.dto.brand.BrandResponse;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.enums.BrandPlanTier;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.BrandRepository;
import com.zingzing.backend.service.BrandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandService brandService;
    private final BrandRepository brandRepository;

    public BrandController(BrandService brandService, BrandRepository brandRepository) {
        this.brandService = brandService;
        this.brandRepository = brandRepository;
    }

    public record PlanSelectRequest(@NotNull @NotBlank String planTier) {}

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
}
