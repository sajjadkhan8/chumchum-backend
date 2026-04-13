package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.user.UserResponse;
import com.chamcham.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getImage(),
                user.getCity(),
                user.getPhone(),
                user.getDescription(),
                user.isSeller(),
                user.getCreatedAt()
        );
    }
}

