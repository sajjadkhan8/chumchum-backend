package com.chamcham.backend.dto.message;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderType,
        String type,
        String content,
        String description,
        boolean isRead,
        String attachmentUrl,
        String offerDealType,
        BigDecimal offerAmount,
        String offerBarterDetails,
        String offerBarterCategory,
        BigDecimal offerEstimatedBarterValue,
        String offerCreatorExpectation,
        String offerMessage,
        String offerStatus,
        Instant createdAt
) {
}

