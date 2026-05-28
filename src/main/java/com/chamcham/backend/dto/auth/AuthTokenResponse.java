package com.chamcham.backend.dto.auth;

import com.chamcham.backend.dto.user.UserResponse;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}

