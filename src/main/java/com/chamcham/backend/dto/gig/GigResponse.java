package com.chamcham.backend.dto.gig;

import com.chamcham.backend.dto.user.UserResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GigResponse(
        UUID id,
        UserResponse seller,
        String title,
        String description,
        int totalStars,
        int starNumber,
        String category,
        BigDecimal price,
        String cover,
        List<String> images,
        String shortTitle,
        String shortDescription,
        String deliveryTime,
        int revisionNumber,
        List<String> features,
        int sales,
        Instant createdAt
) {
}

