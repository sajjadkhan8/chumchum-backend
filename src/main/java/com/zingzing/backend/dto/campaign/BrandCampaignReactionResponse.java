package com.zingzing.backend.dto.campaign;

import java.time.Instant;
import java.util.UUID;

public record BrandCampaignReactionResponse(
        UUID id,
        UUID campaignId,
        String campaignTitle,
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
