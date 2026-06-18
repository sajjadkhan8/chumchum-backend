package com.zingzing.backend.dto.subscription;

import com.zingzing.backend.entity.enums.SubscriptionInterval;
import com.zingzing.backend.entity.enums.SubscriptionStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SubscriptionResponse(
        UUID id,
        UUID brandId,
        UUID packageId,
        String packageTitle,
        SubscriptionStatus status,
        SubscriptionInterval interval,
        int duration,
        int cyclesCompleted,
        Instant nextRenewalAt,
        Instant cancelledAt,
        Instant createdAt
) {
}
