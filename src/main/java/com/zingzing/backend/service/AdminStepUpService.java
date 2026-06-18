package com.zingzing.backend.service;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.config.security.JwtService;
import com.zingzing.backend.entity.User;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminStepUpService {

    private static final String STEP_UP_TYPE = "step_up";
    private static final long STEP_UP_EXPIRY_SECONDS = 300; // 5 minutes

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AdminStepUpService(JwtService jwtService, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    /**
     * Verifies the admin's password and returns a 5-minute step-up token.
     * The token must be sent in X-Step-Up-Token on destructive operations.
     */
    public String issueStepUpToken(AuthenticatedUser authUser, String password) {
        User user = userRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect password");
        }
        return jwtService.generateShortLivedToken(authUser.userId(), STEP_UP_TYPE, STEP_UP_EXPIRY_SECONDS);
    }

    /**
     * Validates a step-up token. Throws 403 if missing, expired, or not issued for the given user.
     * Call this before any destructive admin action that requires re-authentication.
     */
    public void requireStepUp(AuthenticatedUser authUser, String stepUpToken) {
        if (stepUpToken == null || stepUpToken.isBlank()) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "This action requires step-up authentication. Call POST /api/v1/admin/step-up first.");
        }
        try {
            Claims claims = jwtService.parseClaims(stepUpToken);
            if (!STEP_UP_TYPE.equals(claims.get("type", String.class))) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Invalid step-up token");
            }
            if (!authUser.userId().toString().equals(claims.getSubject())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Step-up token was issued for a different session");
            }
        } catch (JwtException ex) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Step-up token is expired or invalid");
        }
    }
}
