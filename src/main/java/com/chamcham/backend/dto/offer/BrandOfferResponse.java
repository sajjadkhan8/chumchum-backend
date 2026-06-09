package com.chamcham.backend.dto.offer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BrandOfferResponse(
        UUID id,
        UUID brandId,
        String brandName,
        String title,
        String brief,
        String offerType,
        Integer budgetMin,
        Integer budgetMax,
        String currency,
        String deliverables,
        String contentFormats,
        String targetPlatforms,
        String categories,
        String niches,
        String tags,
        String requirements,
        String referenceUrls,
        String coverImageUrl,
        LocalDate deadlineDate,
        String targetCity,
        String targetLanguage,
        Integer minFollowers,
        BigDecimal minEngagementRate,
        Integer preferredDeliveryDays,
        Integer slots,
        String visibility,
        String status,
        Instant publishedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        long reactionCount
) {
}

