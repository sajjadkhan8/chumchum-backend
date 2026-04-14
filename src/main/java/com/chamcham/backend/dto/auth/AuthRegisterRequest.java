package com.chamcham.backend.dto.auth;

import com.chamcham.backend.entity.enums.UserRole;
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

        // Creator fields
        @Size(max = 1000) String bio,
        @Size(max = 100) String category,
        @Size(max = 255) String tiktokUrl,
        @Size(max = 255) String instagramUrl,
        @Size(max = 255) String youtubeUrl,

        // Brand fields
        @NotBlank @Size(max = 150) String companyName,
        @Size(max = 255) String website,
        @Size(max = 100) String industry,
        @Size(max = 1000) String description
) {
}
