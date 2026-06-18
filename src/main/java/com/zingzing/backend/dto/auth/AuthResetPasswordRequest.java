package com.zingzing.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 120) String newPassword
) {
}

