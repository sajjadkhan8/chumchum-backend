package com.chamcham.backend.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID packageId,
        UUID creatorId,
        UUID brandId,
        String dealType,
        BigDecimal amount,
        String barterDetails,
        String status,
        int progress,
        LocalDate deadlineDate,
        LocalDate deliveryDate,
        String image,
        String title,
        Instant createdAt
) {
}

