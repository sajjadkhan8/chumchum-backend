package com.zingzing.backend.dto.creator;

import com.zingzing.backend.entity.enums.AvailabilityStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreatorUpdateRequest(
        @Size(max = 100) String name,
        @Size(max = 40) String username,
        @Size(max = 120) String email,
        @Size(max = 20) String phone,
        @Size(max = 80) String city,
        @Size(max = 500) String avatarUrl,
        @Size(max = 1000) String bio,
        @Size(max = 500) String coverImageUrl,
        @Size(max = 300) String website,
        AvailabilityStatus availabilityStatus,
        @Size(max = 50) String responseTime,
        @Min(0) Integer minPrice,
        @Min(0) Integer maxPrice,
        Boolean acceptsBarter,
        Boolean acceptsHybridDeals,
        @Min(0) Integer minimumBudget,
        List<String> languages,
        List<String> categories,
        @Size(max = 255) String tiktokUrl,
        @Size(max = 255) String instagramUrl,
        @Size(max = 255) String youtubeUrl,
        @Size(max = 255) String facebookUrl,
        @Min(0) Integer followers,
        @Min(0) Integer avgViews,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal engagementRate,
        @DecimalMin("0.00") @DecimalMax("5.00") BigDecimal rating,
        @Min(0) Integer totalReviews,
        Boolean isFiler,
        @Min(0) Integer rateCardReel,
        @Min(0) Integer rateCardStory,
        @Min(0) Integer rateCardPost,
        @Min(0) Integer rateCardVideo
) {
}
