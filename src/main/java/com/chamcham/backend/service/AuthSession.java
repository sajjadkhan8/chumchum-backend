package com.chamcham.backend.service;

import com.chamcham.backend.dto.user.UserResponse;

public record AuthSession(String token, UserResponse user) {
}

