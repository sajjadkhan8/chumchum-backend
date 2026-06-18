package com.zingzing.backend.dto.auth;

import com.zingzing.backend.dto.user.UserResponse;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}

