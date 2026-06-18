package com.zingzing.backend.dto.quickdeal;

import java.util.UUID;

public record QuickDealCreateResponse(
        UUID conversationId,
        UUID messageId,
        UUID offerId
) {
}

