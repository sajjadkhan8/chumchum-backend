package com.chamcham.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandProfilePayload(
        @NotBlank @Size(max = 150) String companyName,
        String website,
        String industry,
        @Size(max = 1000)
        String description
) {
}
