package com.chamcham.backend.dto.auth;

import com.chamcham.backend.dto.user.UserResponse;

public record AuthResponse(
        boolean error,
        String message,
        UserResponse user
) {
}

