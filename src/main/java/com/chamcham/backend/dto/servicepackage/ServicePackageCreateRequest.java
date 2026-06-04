package com.chamcham.backend.dto.servicepackage;

import com.chamcham.backend.entity.enums.PackagePlatform;
import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.PackageStatus;
import com.chamcham.backend.entity.enums.PackageType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ServicePackageCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 300) String shortDescription,
        @Size(max = 2000) String description,
        @Size(max = 2000) String fullDescription,
        @NotNull PackagePlatform platform,
        @Size(max = 80) String category,
        @NotNull PackageType type,
        DealType dealType,
        @Size(max = 1000) String barterDetails,
        @Size(max = 1000) String barterDescription,
        @Size(max = 100) String barterCategory,
        @Min(0) Integer estimatedBarterValue,
        @Min(0) Integer hybridCashAmount,
        @Min(0) Integer hybridBarterValue,
        @Size(max = 1000) String creatorExpectations,
        @NotNull @Min(0) Integer price,
        @Size(max = 10) String currency,
        @NotNull @Size(min = 1, max = 30) List<@NotBlank @Size(max = 200) String> deliverables,
        @NotNull @Positive Integer deliveryDays,
        @Positive Integer revisions,
        Boolean isFeatured,
        PackageStatus status,
        String visibility,
        @Size(max = 50) String responseTime,
        String coverImage,
        List<String> mediaUrls,
        List<String> tags,
        Boolean isActive,
        @Valid List<ServicePackageTierRequest> tiers
) {
}


