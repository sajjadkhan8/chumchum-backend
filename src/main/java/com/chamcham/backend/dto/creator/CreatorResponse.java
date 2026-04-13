package com.chamcham.backend.dto.creator;

import com.chamcham.backend.dto.profile.ProfileUserResponse;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreatorResponse(
        UUID id,
        String bio,
        String category,
        String tiktokUrl,
        String instagramUrl,
        String youtubeUrl,
        int followers,
        int avgViews,
        BigDecimal engagementRate,
        BigDecimal rating,
        int totalReviews,
        ProfileUserResponse user,
        Instant createdAt,
        Instant updatedAt
) {
}

