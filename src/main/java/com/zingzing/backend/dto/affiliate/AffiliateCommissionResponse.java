package com.zingzing.backend.dto.affiliate;

import java.time.Instant;
import java.util.UUID;

public record AffiliateCommissionResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        UUID earningCreatorId,
        String earningCreatorName,
        int baseAmount,
        int rateBasisPoints,
        int commissionAmount,
        String status,
        Instant createdAt
) {
}
