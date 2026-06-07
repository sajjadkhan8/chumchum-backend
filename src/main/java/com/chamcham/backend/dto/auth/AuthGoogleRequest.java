package com.chamcham.backend.dto.auth;

import com.chamcham.backend.entity.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthGoogleRequest(
        @NotBlank String idToken,
        @NotNull UserRole role,
        String name
) {
}

