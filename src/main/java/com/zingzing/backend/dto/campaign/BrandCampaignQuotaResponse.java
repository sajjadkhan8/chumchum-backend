package com.zingzing.backend.dto.campaign;

import java.time.Instant;

public record BrandCampaignQuotaResponse(
        String planTier,
        String scope,
        long used,
        Integer limit,
        long remaining,
        boolean unlimited,
        Instant periodStart,
        Instant periodEnd
) {}
