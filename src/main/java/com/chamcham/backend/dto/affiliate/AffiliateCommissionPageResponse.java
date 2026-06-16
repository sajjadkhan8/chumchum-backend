package com.chamcham.backend.dto.affiliate;

import java.util.List;

public record AffiliateCommissionPageResponse(
        List<AffiliateCommissionResponse> commissions,
        long total,
        int page,
        int limit
) {
}
