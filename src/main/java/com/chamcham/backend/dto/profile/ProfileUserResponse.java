package com.chamcham.backend.dto.profile;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProfileUserResponse(
        UUID id,
        String username,
        String image,
        String city,
        String email,
        String phone
) {
}

