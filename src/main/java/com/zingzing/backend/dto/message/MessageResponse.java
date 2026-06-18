package com.zingzing.backend.dto.message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderType,
        String type,
        String content,
        boolean isRead,
        String attachmentUrl,
        String offerDealType,
        Integer offerAmount,
        String offerBarterDetails,
        String offerBarterCategory,
        String offerStatus,
        UUID offerId,
        Integer offerEstimatedBarterValue,
        String offerCreatorExpectation,
        UUID offerOrderId,
        Instant createdAt
) {
}
