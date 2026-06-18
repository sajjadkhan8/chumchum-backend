package com.zingzing.backend.dto.quickdeal;

import java.util.UUID;

public record QuickDealRespondResponse(
        UUID offerId,
        String status,
        UUID orderId
) {
}
