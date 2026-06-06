package com.chamcham.backend.dto.creator;

import com.chamcham.backend.dto.profile.ProfileUserResponse;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.chamcham.backend.entity.enums.CreatorBadgeLevel;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreatorResponse(
        UUID id,
        String name,
        String username,
        String email,
        String phone,
        String city,
        String avatarUrl,
        String bio,
        String category,
        String coverImageUrl,
        String website,
        String niche,
        String availabilityStatus,
        String responseTime,
        Integer minPrice,
        Integer maxPrice,
        boolean isVerified,
        CreatorBadgeLevel badgeLevel,
        boolean isTrending,
        boolean isFastResponder,
        int completedDeals,
        boolean acceptsBarter,
        boolean acceptsHybridDeals,
        Integer minimumBudget,
        String preferredIndustries,
        List<String> languages,
        List<String> categories,
        String tiktokUrl,
        String instagramUrl,
        String youtubeUrl,
        String facebookUrl,
        int followers,
        int avgViews,
        BigDecimal engagementRate,
        BigDecimal rating,
        int totalReviews,
        List<SocialAccountResponse> socialAccounts,
        ProfileUserResponse user,
        Instant createdAt,
        Instant updatedAt
) {
}
