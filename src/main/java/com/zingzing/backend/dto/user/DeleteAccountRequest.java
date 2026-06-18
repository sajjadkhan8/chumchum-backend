package com.zingzing.backend.dto.user;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(@NotBlank String confirmPassword) {
}

