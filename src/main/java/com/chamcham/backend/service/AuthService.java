package com.chamcham.backend.service;

import com.chamcham.backend.config.security.JwtService;
import com.chamcham.backend.dto.auth.*;
import com.chamcham.backend.dto.brand.BrandCreateRequest;
import com.chamcham.backend.dto.creator.CreatorCreateRequest;
import com.chamcham.backend.dto.user.UserResponse;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.CreatorProgramStatus;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.UserMapper;
import com.chamcham.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CreatorService creatorService;
    private final BrandService brandService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final Map<String, UUID> refreshStore = new ConcurrentHashMap<>();
    private final Map<String, UUID> resetTokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, UserMapper userMapper, CreatorService creatorService, BrandService brandService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.creatorService = creatorService;
        this.brandService = brandService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthTokenResponse register(AuthRegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = User.builder()
                .username(generateUsername(request.email()))
                .email(request.email())
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .creatorProgramStatus(request.role() == UserRole.CREATOR ? CreatorProgramStatus.IN_PATH : CreatorProgramStatus.NONE)
                .active(true)
                .build();
        user = userRepository.save(user);

        if (request.role() == UserRole.CREATOR) {
            creatorService.create(new CreatorCreateRequest(user.getId(), null, null, null, null, null, null));
        } else if (request.role() == UserRole.BRAND) {
            brandService.create(new BrandCreateRequest(user.getId(), request.name(), null, null, null));
        }

        return issueTokens(user);
    }

    public AuthTokenResponse login(AuthLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        requireActive(user);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return issueTokens(user);
    }

    public AuthSendOtpResponse sendOtp(AuthSendOtpRequest request) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        otpStore.put(request.phone(), new OtpEntry(otp, Instant.now().plusSeconds(300)));
        return new AuthSendOtpResponse("OTP sent successfully", 300);
    }

    public AuthTokenResponse verifyOtp(AuthVerifyOtpRequest request) {
        OtpEntry entry = otpStore.get(request.phone());
        if (entry == null || entry.expiresAt.isBefore(Instant.now()) || !entry.otp.equals(request.otp())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }
        User user = userRepository.findByPhone(request.phone()).map(existing -> {
            requireActive(existing);
            return existing;
        }).orElseGet(() -> userRepository.save(User.builder()
                .username(generateUsername(request.phone()))
                .phone(request.phone())
                .name(request.phone())
                .email(request.phone() + "@phone.zingzing.sa")
                .role(UserRole.CREATOR)
                .creatorProgramStatus(CreatorProgramStatus.IN_PATH)
                .active(true)
                .build()));
        otpStore.remove(request.phone());
        return issueTokens(user);
    }

    public AuthTokenPair refresh(AuthRefreshRequest request) {
        UUID userId = refreshStore.get(request.refreshToken());
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));
        requireActive(user);
        return new AuthTokenPair(jwtService.generateToken(user.getId(), user.getRole()), request.refreshToken());
    }

    public void forgotPassword(AuthForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> resetTokens.put(UUID.randomUUID().toString(), user.getId()));
    }

    public void resetPassword(AuthResetPasswordRequest request) {
        UUID userId = resetTokens.remove(request.token());
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid/expired reset token");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid/expired reset token"));
        requireActive(user);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshStore.remove(refreshToken);
        }
    }

    public UserResponse me(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        requireActive(user);
        return userMapper.toResponse(user);
    }

    private AuthTokenResponse issueTokens(User user) {
        requireActive(user);
        String accessToken = jwtService.generateToken(user.getId(), user.getRole());
        String refreshToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        refreshStore.put(refreshToken, user.getId());
        return new AuthTokenResponse(accessToken, refreshToken, userMapper.toResponse(user));
    }

    private String generateUsername(String seed) {
        String normalized = seed.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (normalized.length() < 3) {
            normalized = "user" + normalized;
        }
        return normalized.length() > 40 ? normalized.substring(0, 40) : normalized;
    }

    private void requireActive(User user) {
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Account is disabled");
        }
    }

    private record OtpEntry(String otp, Instant expiresAt) {}
    public record AuthTokenPair(String accessToken, String refreshToken) {}
}
