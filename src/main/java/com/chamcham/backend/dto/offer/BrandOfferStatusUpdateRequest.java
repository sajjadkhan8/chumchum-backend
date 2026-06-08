package com.chamcham.backend.dto.offer;

import jakarta.validation.constraints.NotBlank;

public record BrandOfferStatusUpdateRequest(
        @NotBlank String status
) {
}

