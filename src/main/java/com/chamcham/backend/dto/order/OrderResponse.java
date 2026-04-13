package com.chamcham.backend.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID gigId,
        String image,
        String title,
        BigDecimal price,
        UUID sellerId,
        UUID buyerId,
        boolean isCompleted,
        String paymentIntent,
        Instant createdAt
) {
}

