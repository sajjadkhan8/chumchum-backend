package com.zingzing.backend.dto.order;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID packageId,
        String packageTitle,
        UUID creatorId,
        String creatorName,
        UUID brandId,
        String brandName,
        String dealType,
        Integer amount,
        String barterDetails,
        String message,
        String cancellationNote,
        String status,
        int progress,
        OffsetDateTime deadlineDate,
        LocalDate deliveryDate,
        Instant createdAt,
        Instant updatedAt,
        List<DeliverableResponse> deliverables,
        boolean barterProductReceived,
        UUID conversationId,
        boolean hasReviewedByBrand,
        boolean hasReviewedByCreator
) {
}
