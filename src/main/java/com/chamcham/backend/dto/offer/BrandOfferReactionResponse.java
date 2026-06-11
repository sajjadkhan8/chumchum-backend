package com.chamcham.backend.dto.offer;

import java.time.Instant;
import java.util.UUID;

public record BrandOfferReactionResponse(
        UUID id,
        UUID offerId,
        String offerTitle,
        String brandName,
        UUID creatorId,
        String creatorName,
        String creatorAvatar,
        String reactionType,
        String status,
        String message,
        Integer proposedPrice,
        String proposedCurrency,
        Integer proposedDeliveryDays,
        String brandNote,
        String creatorNote,
        UUID orderId,
        Instant createdAt,
        Instant updatedAt
) {
}
