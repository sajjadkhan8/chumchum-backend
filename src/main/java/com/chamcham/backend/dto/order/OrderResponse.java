package com.chamcham.backend.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID packageId,
        String image,
        String title,
        BigDecimal price,
        UUID creatorId,
        UUID brandId,
        boolean isCompleted,
        String paymentIntent,
        Instant createdAt
) {
}

