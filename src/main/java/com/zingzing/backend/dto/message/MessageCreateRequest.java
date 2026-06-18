package com.zingzing.backend.dto.message;

import jakarta.validation.constraints.Size;

public record MessageCreateRequest(
        @Size(max = 2000) String content,
        String offerDealType,
        Integer offerAmount,
        @Size(max = 2000) String offerBarterDetails,
        @Size(max = 100) String offerBarterCategory,
        Integer offerEstimatedBarterValue
) {
}
