package com.chamcham.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AuthVerifyOtpRequest(
        @NotBlank @Pattern(regexp = "^\\+9665\\d{8}$") String phone,
        @NotBlank @Pattern(regexp = "^\\d{6}$") String otp
) {
}

