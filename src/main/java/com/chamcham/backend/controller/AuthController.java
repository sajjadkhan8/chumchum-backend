package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.auth.AuthLoginRequest;
import com.chamcham.backend.dto.auth.AuthRegisterRequest;
import com.chamcham.backend.dto.auth.AuthResponse;
import com.chamcham.backend.dto.user.UserResponse;
import com.chamcham.backend.service.AuthService;
import com.chamcham.backend.service.AuthSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${security.cookie.secure}")
    private boolean secureCookie;

    @Value("${security.cookie.same-site}")
    private String sameSite;

    @Value("${security.cookie.max-age-seconds}")
    private long maxAgeSeconds;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody AuthRegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("error", false, "message", "New user created!"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthSession session = authService.login(request);
        ResponseCookie cookie = ResponseCookie.from("accessToken", session.token())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite(sameSite)
                .maxAge(maxAgeSeconds)
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(false, "Success!", session.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        ResponseCookie cleared = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                .body(Map.of("error", false, "message", "User have been logged out!"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal AuthenticatedUser authUser) {
        UserResponse user = authService.me(authUser.userId());
        return ResponseEntity.ok(new AuthResponse(false, "Success!", user));
    }
}

