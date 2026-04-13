package com.chamcham.backend.service;

import com.chamcham.backend.entity.User;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void deleteUser(UUID targetUserId, UUID actorUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getId().equals(actorUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid request!. Cannot delete other user accounts.");
        }

        userRepository.delete(user);
    }
}

