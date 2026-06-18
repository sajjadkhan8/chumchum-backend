package com.zingzing.backend.dto.auth;

import com.zingzing.backend.entity.enums.UserRole;

import java.util.UUID;

public record AuthRegisterUserResponse(
        UUID id,
        String username,
        String email,
        UserRole role,
        String image,
        String city,
        String phone
) {
}

