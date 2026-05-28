package com.chamcham.backend.service;

import com.chamcham.backend.dto.user.*;
import com.chamcham.backend.entity.NotificationPreference;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.UserMapper;
import com.chamcham.backend.repository.NotificationPreferenceRepository;
import com.chamcham.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       NotificationPreferenceRepository notificationPreferenceRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    public UserResponse me(UUID userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (user.getPasswordHash() != null
                && !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID userId, DeleteAccountRequest request) {
        User user = findUser(userId);
        if (user.getPasswordHash() != null
                && !passwordEncoder.matches(request.confirmPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Password incorrect");
        }
        user.setActive(false);
        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public NotificationPreferencesResponse getNotificationPreferences(UUID userId) {
        User user = findUser(userId);
        return notificationPreferenceRepository.findByUserId(userId)
                .map(this::toPreferencesResponse)
                .orElseGet(() -> defaultPreferences());
    }

    @Transactional
    public NotificationPreferencesResponse saveNotificationPreferences(UUID userId,
                                                                        NotificationPreferencesRequest request) {
        User user = findUser(userId);
        NotificationPreference pref = notificationPreferenceRepository.findByUserId(userId)
                .orElse(NotificationPreference.builder().user(user).build());

        pref.setNewOrders(request.newOrders());
        pref.setMessages(request.messages());
        pref.setReviews(request.reviews());
        pref.setMarketing(request.marketing());
        pref.setWeeklyDigest(request.weeklyDigest());
        pref.setPushNotifications(request.pushNotifications());
        pref.setEmailNotifications(request.emailNotifications());
        pref.setSmsNotifications(request.smsNotifications());

        notificationPreferenceRepository.save(pref);
        return toPreferencesResponse(pref);
    }

    private NotificationPreferencesResponse toPreferencesResponse(NotificationPreference p) {
        return new NotificationPreferencesResponse(
                p.isNewOrders(), p.isMessages(), p.isReviews(), p.isMarketing(),
                p.isWeeklyDigest(), p.isPushNotifications(), p.isEmailNotifications(), p.isSmsNotifications()
        );
    }

    private NotificationPreferencesResponse defaultPreferences() {
        return new NotificationPreferencesResponse(true, true, true, false, true, true, true, false);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
