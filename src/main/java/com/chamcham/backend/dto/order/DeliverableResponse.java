package com.chamcham.backend.dto.order;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DeliverableResponse(
        UUID id,
        UUID orderId,
        String name,
        String status,
        String fileUrl,
        OffsetDateTime submittedAt,
        String revisionNote,
        Instant createdAt
) {
}
