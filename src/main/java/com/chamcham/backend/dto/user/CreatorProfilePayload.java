package com.chamcham.backend.dto.user;

import java.math.BigDecimal;

public record CreatorProfilePayload(
        String bio,
        String category,
        String tiktokUrl,
        String instagramUrl,
        String youtubeUrl,
        Integer followers,
        Integer avgViews,
        BigDecimal engagementRate,
        BigDecimal rating,
        Integer totalReviews
) {
}
