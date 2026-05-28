package com.chamcham.backend.dto.brand;

import com.chamcham.backend.dto.profile.ProfileUserResponse;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BrandResponse(
        UUID id,
        String name,
        String website,
        String industry,
        String description,
        String logoUrl,
        Integer monthlyBudget,
        ProfileUserResponse user,
        Instant createdAt,
        Instant updatedAt
) {
}
