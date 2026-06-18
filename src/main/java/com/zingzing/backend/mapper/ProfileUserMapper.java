package com.zingzing.backend.mapper;

import com.zingzing.backend.dto.profile.ProfileUserResponse;
import com.zingzing.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ProfileUserMapper {

    public ProfileUserResponse toResponse(User user) {
        return new ProfileUserResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getImage(),
                user.getCity(),
                user.getEmail(),
                user.getPhone()
        );
    }

    public ProfileUserResponse toPublicResponse(User user) {
        return new ProfileUserResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getImage(),
                user.getCity(),
                null,
                null
        );
    }
}
