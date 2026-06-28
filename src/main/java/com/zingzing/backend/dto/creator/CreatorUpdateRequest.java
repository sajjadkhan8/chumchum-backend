package com.zingzing.backend.dto.creator;

import com.zingzing.backend.entity.enums.AvailabilityStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

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
        @Min(0) Integer minimumBudget,
        List<String> languages,
        List<String> categories,
        @Size(max = 255) String tiktokUrl,
        @Size(max = 255) String instagramUrl,
        @Size(max = 255) String youtubeUrl,
        @Size(max = 255) String facebookUrl,
        @Size(max = 255) String snapchatUrl,
        Boolean isFiler,
        @Min(0) Integer rateCardReel,
        @Min(0) Integer rateCardStory,
        @Min(0) Integer rateCardPost,
        @Min(0) Integer rateCardVideo,
        List<String> collaborationPreferences,
        List<String> barterTypes
) {
}
