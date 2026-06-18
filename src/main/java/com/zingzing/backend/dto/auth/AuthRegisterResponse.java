package com.zingzing.backend.dto.auth;

public record AuthRegisterResponse(
        AuthRegisterUserResponse user,
        String message
) {
}

