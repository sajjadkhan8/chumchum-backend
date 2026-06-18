package com.zingzing.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AuthSendOtpRequest(
        @NotBlank @Pattern(regexp = "^\\+923\\d{9}$") String phone
) {
}
