package com.chamcham.backend.dto.servicepackage;

import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.PackagePlatform;
import com.chamcham.backend.entity.enums.PackageStatus;
import com.chamcham.backend.entity.enums.PackageType;
import com.chamcham.backend.entity.enums.SubscriptionInterval;
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
        String category,
        PackageType type,
        DealType dealType,
        String barterDetails,
        String barterDescription,
        String barterCategory,
        Integer estimatedBarterValue,
        Integer hybridCashAmount,
        Integer hybridBarterValue,
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
        List<ServicePackageTierResponse> tiers,
        SubscriptionInterval subscriptionInterval,
        Integer subscriptionDuration,
        Instant createdAt,
        Instant updatedAt
) {
}



