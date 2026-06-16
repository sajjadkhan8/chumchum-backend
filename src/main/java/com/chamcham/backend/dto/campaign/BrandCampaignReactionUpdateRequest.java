package com.chamcham.backend.dto.campaign;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record BrandCampaignReactionUpdateRequest(
        @Size(max = 2000) String message,
        @Min(0) Integer proposedPrice,
        @Size(max = 10) String proposedCurrency,
        @Min(1) Integer proposedDeliveryDays,
        @Size(max = 2000) String creatorNote,
        String status
) {
}
