package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.user.ChangePasswordRequest;
import com.chamcham.backend.dto.user.DeleteAccountRequest;
import com.chamcham.backend.dto.user.NotificationPreferencesRequest;
import com.chamcham.backend.dto.user.NotificationPreferencesResponse;
import com.chamcham.backend.dto.user.UserResponse;
import com.chamcham.backend.service.UserService;
import com.chamcham.backend.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(userService.me(authUser.userId())));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal AuthenticatedUser authUser, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe(@AuthenticationPrincipal AuthenticatedUser authUser, @Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteUser(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/me/notification-preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> getPreferences(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getNotificationPreferences(authUser.userId())));
    }

    @PutMapping("/me/notification-preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> updatePreferences(@AuthenticationPrincipal AuthenticatedUser authUser, @Valid @RequestBody NotificationPreferencesRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.saveNotificationPreferences(authUser.userId(), request)));
    }
}

