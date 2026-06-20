package com.zingzing.backend.dto.creator;

import com.zingzing.backend.entity.enums.VerificationSource;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SocialAccountResponse(
        UUID id,
        String platform,
        String username,
        String profileUrl,
        int followers,
        Integer avgViews,
        BigDecimal engagementRate,
        boolean isVerified,
        VerificationSource verifiedBy,
        String oauthStatus,
        Instant lastSyncedAt,
        String syncError
) {
}
