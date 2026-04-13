package com.chamcham.backend.dto.conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID sellerId,
        UUID buyerId,
        boolean readBySeller,
        boolean readByBuyer,
        String lastMessage,
        Instant updatedAt
) {
}

