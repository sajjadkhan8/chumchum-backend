package com.chamcham.backend.dto.user;

import com.chamcham.backend.entity.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String image,
        String city,
        String phone,
        UserRole role,
        boolean isSeller,
        boolean active,
        CreatorProfilePayload creator,
        BrandProfilePayload brand,
        Instant createdAt
) {
}

