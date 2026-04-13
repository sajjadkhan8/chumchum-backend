package com.chamcham.backend.dto.servicepackage;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ServicePackageTierResponse(
        UUID id,
        String name,
        BigDecimal price,
        String deliverables,
        Integer deliveryDays,
        Integer revisions,
        Instant createdAt
) {
}

