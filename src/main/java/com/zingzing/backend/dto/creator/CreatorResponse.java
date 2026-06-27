package com.zingzing.backend.dto.creator;

import com.zingzing.backend.dto.profile.ProfileUserResponse;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.zingzing.backend.entity.enums.AvailabilityStatus;
import com.zingzing.backend.entity.enums.CreatorBadgeLevel;

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
        String coverImageUrl,
        String website,
        AvailabilityStatus availabilityStatus,
        String responseTime,
        Integer minPrice,
        Integer maxPrice,
        boolean isVerified,
        String verificationStatus,
        CreatorBadgeLevel badgeLevel,
        boolean isTrending,
        boolean isFastResponder,
        int completedDeals,
        int completionRate,
        int repeatClients,
        Integer minimumBudget,
        List<String> languages,
        List<String> categories,
        int followers,
        int avgViews,
        BigDecimal engagementRate,
        BigDecimal rating,
        int totalReviews,
        List<SocialAccountResponse> socialAccounts,
        List<ContentPreviewResponse> contentPreviews,
        ProfileUserResponse user,
        boolean isFiler,
        Integer rateCardReel,
        Integer rateCardStory,
        Integer rateCardPost,
        Integer rateCardVideo,
        int activeOrderCount,
        Instant createdAt,
        Instant updatedAt,
        List<String> collaborationPreferences,
        List<String> barterTypes
) {
}
