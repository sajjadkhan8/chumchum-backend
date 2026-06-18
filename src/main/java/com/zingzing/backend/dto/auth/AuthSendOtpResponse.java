package com.zingzing.backend.dto.auth;

public record AuthSendOtpResponse(String message, int expiresIn) {
}

