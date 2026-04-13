package com.chamcham.backend.dto.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String image,
        String country,
        String phone,
        String description,
        boolean isSeller,
        Instant createdAt
) {
}

