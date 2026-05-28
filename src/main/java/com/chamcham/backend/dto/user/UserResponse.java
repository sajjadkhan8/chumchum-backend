package com.chamcham.backend.dto.user;


import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String role,
        String avatarUrl,
        String creatorProgramStatus,
        String city,
        String phone,
        CreatorProfilePayload creator,
        BrandProfilePayload brand,
        boolean active,
        Instant createdAt
) {
}

