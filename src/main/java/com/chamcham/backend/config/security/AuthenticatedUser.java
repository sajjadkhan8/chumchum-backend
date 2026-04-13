package com.chamcham.backend.config.security;

import com.chamcham.backend.entity.enums.UserRole;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        UserRole role
) {

    public boolean creator() {
        return role.isCreator();
    }

    public boolean brand() {
        return role.isBrand();
    }
}

