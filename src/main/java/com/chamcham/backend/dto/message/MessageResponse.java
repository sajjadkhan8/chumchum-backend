package com.chamcham.backend.dto.message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String description,
        Instant createdAt
) {
}

