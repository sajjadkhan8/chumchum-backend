package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.auth.*;
import com.chamcham.backend.service.AuthService;
import com.chamcham.backend.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> google(@Valid @RequestBody AuthGoogleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.authenticateWithGoogle(request)));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<AuthSendOtpResponse>> sendOtp(@Valid @RequestBody AuthSendOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.sendOtp(request)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> verifyOtp(@Valid @RequestBody AuthVerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyOtp(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthService.AuthTokenPair>> refresh(@Valid @RequestBody AuthRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(@Valid @RequestBody AuthForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "If your email exists, a reset link has been sent.")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody AuthResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>ok(null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal AuthenticatedUser authUser) {
        authService.logout(null);
        return ResponseEntity.ok(ApiResponse.<Void>ok(null));
    }
}

