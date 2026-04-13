package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.servicepackage.ServicePackageCreateRequest;
import com.chamcham.backend.dto.servicepackage.ServicePackageResponse;
import com.chamcham.backend.service.ServicePackageService;
import com.chamcham.backend.util.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/packages")
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePackage(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        servicePackageService.deletePackage(id, authUser.userId(), authUser.role());
        return ResponseEntity.ok(Map.of("error", false, "message", "Package has been deleted successfully!"));
    }

    @GetMapping("/single/{id}")
    public ResponseEntity<ServicePackageResponse> getPackage(@PathVariable UUID id) {
        return ResponseEntity.ok(servicePackageService.getPackage(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ServicePackageResponse>> getPackages(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
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
}



