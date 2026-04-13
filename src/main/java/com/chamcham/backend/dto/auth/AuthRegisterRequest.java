package com.chamcham.backend.dto.auth;

import com.chamcham.backend.dto.user.BrandProfilePayload;
import com.chamcham.backend.dto.user.CreatorProfilePayload;
import com.chamcham.backend.entity.enums.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
        @NotBlank @Size(min = 3, max = 40) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 120) String password,
        String image,
        String city,
        String phone,
        @NotNull UserRole role,
        @Valid CreatorProfilePayload creator,
        @Valid BrandProfilePayload brand
) {
}

