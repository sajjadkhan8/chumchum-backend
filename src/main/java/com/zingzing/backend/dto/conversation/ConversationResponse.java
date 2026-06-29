package com.zingzing.backend.dto.conversation;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID creatorId,
        UUID brandId,
        String contextType,
        UUID contextId,
        String contextLabel,
        String contextTitle,
        String contextStatus,
        Integer contextAmount,
        OffsetDateTime contextDeadlineDate,
        int unreadCountCreator,
        int unreadCountBrand,
        String lastMessage,
        Instant updatedAt,
        String creatorName,
        String creatorAvatarUrl,
        boolean creatorOnline,
        Instant creatorLastSeenAt,
        String brandName,
        String brandLogoUrl,
        boolean brandOnline,
        Instant brandLastSeenAt,
        boolean blockedByMe,
        boolean blockedByThem
) {
}
