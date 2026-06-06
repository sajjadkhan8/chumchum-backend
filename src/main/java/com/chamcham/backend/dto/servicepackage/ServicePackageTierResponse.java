package com.chamcham.backend.dto.servicepackage;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ServicePackageTierResponse(
        UUID id,
        String name,
        Integer price,  // PKR amount
        String description,
        List<String> deliverables,
        Integer deliveryDays,
        Integer revisions,
        Integer position,
        Boolean isPrimary,
        Instant createdAt,
        Instant updatedAt
) {
}

