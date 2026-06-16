package com.chamcham.backend.dto.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandCampaignReactionActionRequest(
        @NotBlank String action,
        @Size(max = 2000) String brandNote
) {
}
