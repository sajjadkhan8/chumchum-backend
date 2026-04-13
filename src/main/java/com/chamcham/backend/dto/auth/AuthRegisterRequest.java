package com.chamcham.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
        @NotBlank @Size(min = 3, max = 40) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 120) String password,
        String image,
        @NotBlank String city,
        @NotBlank String phone,
        @NotBlank @Size(max = 1000) String description,
        boolean isSeller
) {
}

