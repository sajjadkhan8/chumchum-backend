package com.chamcham.backend.config.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        boolean seller
) {
}

