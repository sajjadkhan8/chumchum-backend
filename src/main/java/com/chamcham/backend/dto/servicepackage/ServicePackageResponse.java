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
        String shortDescription,
        String description,
        String fullDescription,
        PackagePlatform platform,
        String category,
        PackageType type,
        PackagePricingType pricingType,
        String barterDetails,
        String barterDescription,
        String barterCategory,
        BigDecimal estimatedBarterValue,
        BigDecimal hybridCashAmount,
        BigDecimal hybridBarterValue,
        String creatorExpectation,
        BigDecimal price,
        String currency,
        String deliverables,
        int deliveryDays,
        Integer durationDays,
        int revisions,
        boolean isActive,
        boolean isFeatured,
        String status,
        String visibility,
        boolean isPopular,
        int ordersCompleted,
        String responseTime,
        String coverImage,
        List<String> mediaUrls,
        List<String> tags,
        List<ServicePackageTierResponse> tiers,
        Instant createdAt,
        Instant updatedAt
) {
}



