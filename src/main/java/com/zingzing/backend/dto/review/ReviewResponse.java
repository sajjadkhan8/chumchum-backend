package com.zingzing.backend.dto.review;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID orderId,
        UUID creatorId,
        UUID brandId,
        String brandName,
        String brandLogoUrl,
        String reviewerType,
        int rating,
        String comment,
        Instant createdAt
) {
}
