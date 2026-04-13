package com.chamcham.backend.dto.auth;

public record AuthRegisterResponse(
        AuthRegisterUserResponse user,
        String message
) {
}

