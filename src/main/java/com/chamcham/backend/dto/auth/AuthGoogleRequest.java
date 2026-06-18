package com.chamcham.backend.dto.auth;

import com.chamcham.backend.entity.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthGoogleRequest(
        @NotBlank String idToken,
        @NotNull UserRole role,
        String name,
        String affiliateCode,
        Boolean termsAccepted
) {
    public AuthGoogleRequest(String idToken, UserRole role, String name) {
        this(idToken, role, name, null, null);
    }
}
