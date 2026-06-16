package com.chamcham.backend.dto.affiliate;

public record AffiliateOverviewResponse(
        String code,
        String shareUrl,
        int rateBasisPoints,
        int totalCommission,
        long referredCreators,
        long commissionCount
) {
}
