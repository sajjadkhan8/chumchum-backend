package com.chamcham.backend.dto.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String body,
        String entityType,
        UUID entityId,
        boolean read,
        Instant createdAt
) {
}

