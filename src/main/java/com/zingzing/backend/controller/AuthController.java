package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.auth.*;
import com.zingzing.backend.dto.auth.*;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.service.AdminStepUpService;
import com.zingzing.backend.service.AuthService;
import com.zingzing.backend.util.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "zingzing-refresh-token";

    private final AuthService authService;
    private final AdminStepUpService adminStepUpService;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final long cookieMaxAgeSeconds;

    public AuthController(
            AuthService authService,
            AdminStepUpService adminStepUpService,
            @Value("${security.cookie.secure:true}") boolean cookieSecure,
            @Value("${security.cookie.same-site:Strict}") String cookieSameSite,
            @Value("${security.cookie.max-age-seconds:2592000}") long cookieMaxAgeSeconds
    ) {
        this.authService = authService;
        this.adminStepUpService = adminStepUpService;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.cookieMaxAgeSeconds = cookieMaxAgeSeconds;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> register(@Valid @RequestBody AuthRegisterRequest request) {
        return tokenResponse(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        if (result.mfaRequired()) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "mfaRequired", true,
                    "challengeToken", result.mfaChallengeToken()
            )));
        }
        return tokenResponse(result.tokens(), HttpStatus.OK);
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> google(@Valid @RequestBody AuthGoogleRequest request) {
        return tokenResponse(authService.authenticateWithGoogle(request), HttpStatus.OK);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<AuthSendOtpResponse>> sendOtp(@Valid @RequestBody AuthSendOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.sendOtp(request)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> verifyOtp(@Valid @RequestBody AuthVerifyOtpRequest request) {
        return tokenResponse(authService.verifyOtp(request), HttpStatus.OK);
    }

    // HIGH-8: Second step of admin MFA login
    public record MfaChallengeRequest(@NotBlank String challengeToken, @NotBlank @Size(min = 6, max = 6) String totpCode) {}

    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> verifyMfa(@Valid @RequestBody MfaChallengeRequest request) {
        return tokenResponse(authService.verifyMfaChallenge(request.challengeToken(), request.totpCode()), HttpStatus.OK);
    }

    // HIGH-8: MFA management — authenticated admin only
    public record MfaCodeRequest(@NotBlank @Size(min = 6, max = 6) String totpCode) {}

    @PostMapping("/admin/mfa/setup")
    public ResponseEntity<ApiResponse<AuthService.MfaSetupResponse>> setupMfa(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        requireAdminPrincipal(authUser);
        return ResponseEntity.ok(ApiResponse.ok(authService.setupMfa(authUser.userId())));
    }

    @PostMapping("/admin/mfa/enable")
    public ResponseEntity<ApiResponse<Void>> enableMfa(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody MfaCodeRequest request
    ) {
        requireAdminPrincipal(authUser);
        authService.enableMfa(authUser.userId(), request.totpCode());
        return ResponseEntity.ok(ApiResponse.<Void>ok(null));
    }

    @PostMapping("/admin/mfa/disable")
    public ResponseEntity<ApiResponse<Void>> disableMfa(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody MfaCodeRequest request
    ) {
        requireAdminPrincipal(authUser);
        authService.disableMfa(authUser.userId(), request.totpCode());
        return ResponseEntity.ok(ApiResponse.<Void>ok(null));
    }

    // HIGH-9: Admin step-up re-authentication for destructive operations
    public record StepUpRequest(@NotBlank String password) {}

    @PostMapping("/admin/step-up")
    public ResponseEntity<ApiResponse<Map<String, String>>> stepUp(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody StepUpRequest request
    ) {
        requireAdminPrincipal(authUser);
        String token = adminStepUpService.issueStepUpToken(authUser, request.password());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("stepUpToken", token)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthService.AuthTokenPair>> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String cookieToken,
            @RequestBody(required = false) AuthRefreshRequest request
    ) {
        String refreshToken = cookieToken != null ? cookieToken : request == null ? null : request.refreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        AuthService.AuthTokenPair pair = authService.refresh(new AuthRefreshRequest(refreshToken));
        // Set the rotated refresh token as a new cookie; omit it from the response body
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(pair.refreshToken(), Duration.ofSeconds(cookieMaxAgeSeconds)).toString())
                .body(ApiResponse.ok(new AuthService.AuthTokenPair(pair.accessToken(), null)));
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
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String cookieToken,
            @RequestBody(required = false) AuthRefreshRequest request
    ) {
        String refreshToken = cookieToken != null ? cookieToken : request == null ? null : request.refreshToken();
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .body(ApiResponse.<Void>ok(null));
    }

    private ResponseEntity<ApiResponse<AuthTokenResponse>> tokenResponse(AuthTokenResponse token, HttpStatus status) {
        AuthTokenResponse browserResponse = new AuthTokenResponse(token.accessToken(), null, token.user());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(token.refreshToken(), Duration.ofSeconds(cookieMaxAgeSeconds)).toString())
                .body(ApiResponse.ok(browserResponse));
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }

    private void requireAdminPrincipal(AuthenticatedUser authUser) {
        if (authUser == null || !authUser.role().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
