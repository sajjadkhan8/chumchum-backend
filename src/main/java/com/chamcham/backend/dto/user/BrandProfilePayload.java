package com.chamcham.backend.dto.user;

public record BrandProfilePayload(
        String companyName,
        String website,
        String industry,
        String description
) {
}
