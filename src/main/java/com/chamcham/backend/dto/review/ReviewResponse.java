package com.chamcham.backend.dto.review;

import com.chamcham.backend.dto.user.UserResponse;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID packageId,
        UserResponse reviewer,
        int star,
        String description,
        Instant createdAt
) {
}

