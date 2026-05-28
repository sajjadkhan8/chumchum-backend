package com.chamcham.backend.dto.review;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID orderId,
        UUID creatorId,
        UUID brandId,
        int rating,
        String comment,
        Instant createdAt
) {
}
