package com.chamcham.backend.dto.conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID creatorId,
        UUID brandId,
        boolean readByCreator,
        boolean readByBrand,
        String lastMessage,
        Instant updatedAt
) {
}

