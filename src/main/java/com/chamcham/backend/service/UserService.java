package com.chamcham.backend.service;

import com.chamcham.backend.dto.user.*;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.UserMapper;
import com.chamcham.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final Map<UUID, NotificationPreferencesResponse> preferencesStore = new ConcurrentHashMap<>();

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        return userMapper.toResponse(user);
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public void deleteUser(UUID userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(request.confirmPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password incorrect");
        }
        user.setActive(false);
        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    public NotificationPreferencesResponse getNotificationPreferences(UUID userId) {
        return preferencesStore.getOrDefault(userId, defaultPreferences());
    }

    public NotificationPreferencesResponse saveNotificationPreferences(UUID userId, NotificationPreferencesRequest request) {
        NotificationPreferencesResponse response = new NotificationPreferencesResponse(
                request.newOrders(), request.messages(), request.reviews(), request.marketing(),
                request.weeklyDigest(), request.pushNotifications(), request.emailNotifications(), request.smsNotifications()
        );
        preferencesStore.put(userId, response);
        return response;
    }

    private NotificationPreferencesResponse defaultPreferences() {
        return new NotificationPreferencesResponse(true, true, true, false, true, true, true, false);
    }
}

