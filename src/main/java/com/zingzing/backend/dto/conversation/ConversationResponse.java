package com.zingzing.backend.dto.conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID creatorId,
        UUID brandId,
        int unreadCountCreator,
        int unreadCountBrand,
        String lastMessage,
        Instant updatedAt,
        String creatorName,
        String creatorAvatarUrl,
        String brandName,
        String brandLogoUrl,
        boolean blockedByMe,
        boolean blockedByThem
) {
}
