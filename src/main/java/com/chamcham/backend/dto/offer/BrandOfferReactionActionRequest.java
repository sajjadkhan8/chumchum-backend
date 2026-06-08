package com.chamcham.backend.dto.offer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandOfferReactionActionRequest(
        @NotBlank String action,
        @Size(max = 2000) String brandNote
) {
}

