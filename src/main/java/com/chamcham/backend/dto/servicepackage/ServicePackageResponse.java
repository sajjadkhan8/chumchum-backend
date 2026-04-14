package com.chamcham.backend.dto.servicepackage;

import com.chamcham.backend.dto.profile.ProfileUserResponse;
import com.chamcham.backend.entity.enums.PackagePlatform;
import com.chamcham.backend.entity.enums.PackagePricingType;
import com.chamcham.backend.entity.enums.PackageType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ServicePackageResponse(
        UUID id,
        UUID creatorId,
        String name,
        String title,
        String description,
        PackagePlatform platform,
        String category,
        PackageType type,
        PackagePricingType pricingType,
        String barterDetails,
        BigDecimal price,
        String currency,
        String deliverables,
        int deliveryDays,
        Integer durationDays,
        int revisions,
        boolean isActive,
        boolean isFeatured,
        String coverImage,
        List<String> mediaUrls,
        List<String> tags,
        List<ServicePackageTierResponse> tiers,
        Instant createdAt,
        Instant updatedAt
) {
}



