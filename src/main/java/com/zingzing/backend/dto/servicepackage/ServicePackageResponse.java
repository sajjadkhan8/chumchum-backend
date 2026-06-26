package com.zingzing.backend.dto.servicepackage;

import com.zingzing.backend.entity.enums.DealType;
import com.zingzing.backend.entity.enums.PackageCategory;
import com.zingzing.backend.entity.enums.PackagePlatform;
import com.zingzing.backend.entity.enums.PackageStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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
        PackageCategory category,
        DealType dealType,
        String barterDetails,
        String barterDescription,
        Integer hybridCashAmount,
        String creatorExpectations,
        Integer price,
        String currency,
        List<String> deliverables,
        int deliveryDays,
        int revisions,
        boolean isActive,
        boolean isFeatured,
        PackageStatus status,
        String visibility,
        boolean isPopular,
        int ordersCompleted,
        String responseTime,
        String coverImage,
        List<String> mediaUrls,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
}
