package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.servicepackage.ServicePackageCreateRequest;
import com.zingzing.backend.dto.servicepackage.ServicePackageResponse;
import com.zingzing.backend.entity.enums.PackageCategory;
import com.zingzing.backend.service.ServicePackageService;
import com.zingzing.backend.util.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/packages")
public class PackageController {

    private final ServicePackageService servicePackageService;

    public PackageController(ServicePackageService servicePackageService) {
        this.servicePackageService = servicePackageService;
    }

    @PostMapping
    public ResponseEntity<ServicePackageResponse> createPackage(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody ServicePackageCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicePackageService.createPackage(authUser.userId(), authUser.role(), request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServicePackageResponse> updatePackage(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody ServicePackageCreateRequest request
    ) {
        return ResponseEntity.ok(servicePackageService.updatePackage(id, authUser.userId(), authUser.role(), request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ServicePackageResponse> updatePackageStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody Map<String, String> request
    ) {
        String status = request.get("status");
        return ResponseEntity.ok(servicePackageService.updateStatus(id, authUser.userId(), authUser.role(), status));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ServicePackageResponse> duplicatePackage(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicePackageService.duplicate(id, authUser.userId(), authUser.role()));
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(servicePackageService.getAnalytics(id, authUser.userId(), authUser.role()));
    }

    @PostMapping("/{id}/track")
    public ResponseEntity<Map<String, Object>> trackPackageEvent(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> request
    ) {
        String eventType = request == null ? "VIEW" : request.getOrDefault("eventType", "VIEW");
        String source = request == null ? "package_detail" : request.getOrDefault("source", "package_detail");
        servicePackageService.trackPackageEvent(id, eventType, source);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePackage(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        servicePackageService.deletePackage(id, authUser.userId(), authUser.role());
        return ResponseEntity.ok(Map.of("error", false, "message", "Package has been deleted successfully!"));
    }

    @GetMapping("/mine")
    public ResponseEntity<PageResponse<ServicePackageResponse>> getMyPackages(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt") String sort
    ) {
        return ResponseEntity.ok(servicePackageService.getMyPackages(authUser.userId(), authUser.role(), page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicePackageResponse> getPackage(@PathVariable UUID id) {
        return ResponseEntity.ok(servicePackageService.getPackage(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ServicePackageResponse>> getPackages(
            @RequestParam(required = false) PackageCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer min,
            @RequestParam(required = false) Integer max,
            @RequestParam(required = false) UUID creatorId,
            @RequestParam(required = false) UUID creatorUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort
    ) {
        return ResponseEntity.ok(servicePackageService.getPackages(
                category,
                search,
                min,
                max,
                creatorId,
                creatorUserId,
                page,
                size,
                sort
        ));
    }

    @GetMapping("/featured")
    public ResponseEntity<PageResponse<ServicePackageResponse>> getFeaturedPackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(servicePackageService.getFeaturedPackages(page, size));
    }
}
