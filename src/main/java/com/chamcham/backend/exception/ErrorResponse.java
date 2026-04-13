package com.chamcham.backend.exception;

import java.time.Instant;

public record ErrorResponse(
        boolean error,
        String message,
        Instant timestamp
) {
}

