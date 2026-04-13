package com.chamcham.backend.config.security;

import com.chamcham.backend.entity.UserRole;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        UserRole role
) {

    public boolean seller() {
        return role.isCreator();
    }

    public boolean brand() {
        return role.isBrand();
    }
}

