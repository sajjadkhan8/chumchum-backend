package com.chamcham.backend.dto.conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID creatorId,
        UUID brandId,
        boolean readByCreator,
        boolean readByBrand,
        int unreadCountCreator,
        int unreadCountBrand,
        String lastMessage,
        Instant updatedAt,
        String creatorName,
        String creatorAvatarUrl,
        String brandName,
        String brandLogoUrl
) {
}
