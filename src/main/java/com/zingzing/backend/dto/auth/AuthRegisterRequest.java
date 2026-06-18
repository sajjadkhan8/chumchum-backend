package com.zingzing.backend.dto.auth;

import com.zingzing.backend.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 120)
        @Pattern(regexp = ".*(?=.*[A-Z])(?=.*\\d).*") String password,
        @NotNull UserRole role,
        @Size(max = 40) String affiliateCode,
        Boolean termsAccepted
) {
    public AuthRegisterRequest(String name, String email, String password, UserRole role) {
        this(name, email, password, role, null, null);
    }
}
