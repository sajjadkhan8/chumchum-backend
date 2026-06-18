package com.zingzing.backend.dto.auth;

import com.zingzing.backend.dto.user.UserResponse;

public record AuthResponse(
        boolean error,
        String message,
        UserResponse user
) {
}

