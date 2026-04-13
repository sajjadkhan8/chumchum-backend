package com.chamcham.backend.dto.conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID sellerId,
        UUID brandId,
        boolean readBySeller,
        boolean readByBrand,
        String lastMessage,
        Instant updatedAt
) {
}

