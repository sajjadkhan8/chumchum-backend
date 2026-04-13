package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.profile.ProfileUserResponse;
import com.chamcham.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ProfileUserMapper {

    public ProfileUserResponse toResponse(User user) {
        return new ProfileUserResponse(
                user.getId(),
                user.getUsername(),
                user.getImage(),
                user.getCity(),
                user.getEmail(),
                user.getPhone()
        );
    }
}

