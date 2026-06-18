package com.zingzing.backend.service;

import com.zingzing.backend.dto.user.UserResponse;

public record AuthSession(String token, UserResponse user) {
}

