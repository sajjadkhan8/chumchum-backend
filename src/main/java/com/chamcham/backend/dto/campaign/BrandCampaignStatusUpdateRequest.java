package com.chamcham.backend.dto.campaign;

import jakarta.validation.constraints.NotBlank;

public record BrandCampaignStatusUpdateRequest(
        @NotBlank String status
) {
}
