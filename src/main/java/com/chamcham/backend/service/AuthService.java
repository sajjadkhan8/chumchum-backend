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
    private final GoogleTokenVerifierService googleTokenVerifierService;

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final Map<String, UUID> refreshStore = new ConcurrentHashMap<>();
    private final Map<String, UUID> resetTokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       UserMapper userMapper,
                       CreatorService creatorService,
                       BrandService brandService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       GoogleTokenVerifierService googleTokenVerifierService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.creatorService = creatorService;
        this.brandService = brandService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifierService = googleTokenVerifierService;
    }

    @Transactional
    public AuthTokenResponse register(AuthRegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = User.builder()
                .username(generateUniqueUsername(request.email()))
                .email(request.email())
                .emailVerified(false)
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

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "This account uses social login. Continue with Google.");
        }

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

    @Transactional
    public AuthTokenResponse authenticateWithGoogle(AuthGoogleRequest request) {
        if (request.role() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role is required for Google authentication");
        }

        GoogleTokenVerifierService.VerifiedGoogleProfile googleProfile = googleTokenVerifierService.verifyIdToken(request.idToken());

        User user = userRepository.findByGoogleSubject(googleProfile.subject())
                .map(existing -> updateLinkedGoogleUser(existing, googleProfile, request))
                .orElseGet(() -> userRepository.findByEmail(googleProfile.email())
                        .map(existing -> linkGoogleToExistingEmail(existing, googleProfile, request))
                        .orElseGet(() -> createGoogleUser(googleProfile, request)));

        requireActive(user);
        return issueTokens(user);
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

    private String generateUniqueUsername(String seed) {
        String base = generateUsername(seed);
        if (!userRepository.existsByUsername(base)) {
            return base;
        }

        for (int i = 1; i <= 9999; i++) {
            String suffix = String.valueOf(i);
            int maxBaseLength = 40 - suffix.length();
            String candidate = base.substring(0, Math.min(base.length(), maxBaseLength)) + suffix;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        return base.substring(0, Math.min(base.length(), 32)) + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private User updateLinkedGoogleUser(User existing,
                                        GoogleTokenVerifierService.VerifiedGoogleProfile googleProfile,
                                        AuthGoogleRequest request) {
        requireActive(existing);
        if (request.role() != null && request.role() != existing.getRole()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This Google account is already linked to another role");
        }
        if (existing.getEmail() == null || existing.getEmail().isBlank()) {
            existing.setEmail(googleProfile.email());
        }
        existing.setEmailVerified(true);
        if ((existing.getName() == null || existing.getName().isBlank()) && googleProfile.name() != null && !googleProfile.name().isBlank()) {
            existing.setName(googleProfile.name());
        }
        if ((existing.getAvatarUrl() == null || existing.getAvatarUrl().isBlank()) && googleProfile.picture() != null && !googleProfile.picture().isBlank()) {
            existing.setAvatarUrl(googleProfile.picture());
        }
        return userRepository.save(existing);
    }

    private User linkGoogleToExistingEmail(User existing,
                                           GoogleTokenVerifierService.VerifiedGoogleProfile googleProfile,
                                           AuthGoogleRequest request) {
        requireActive(existing);
        if (request.role() != null && request.role() != existing.getRole()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email already exists with another role");
        }
        if (existing.getGoogleSubject() != null && !existing.getGoogleSubject().equals(googleProfile.subject())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already linked with another Google account");
        }

        existing.setGoogleSubject(googleProfile.subject());
        existing.setEmailVerified(true);
        if ((existing.getName() == null || existing.getName().isBlank()) && googleProfile.name() != null && !googleProfile.name().isBlank()) {
            existing.setName(googleProfile.name());
        }
        if ((existing.getAvatarUrl() == null || existing.getAvatarUrl().isBlank()) && googleProfile.picture() != null && !googleProfile.picture().isBlank()) {
            existing.setAvatarUrl(googleProfile.picture());
        }
        return userRepository.save(existing);
    }

    private User createGoogleUser(GoogleTokenVerifierService.VerifiedGoogleProfile googleProfile,
                                  AuthGoogleRequest request) {
        UserRole role = request.role();
        String displayName = request.name() != null && !request.name().isBlank()
                ? request.name().trim()
                : (googleProfile.name() != null && !googleProfile.name().isBlank() ? googleProfile.name().trim() : googleProfile.email());

        User user = User.builder()
                .username(generateUniqueUsername(googleProfile.email()))
                .email(googleProfile.email())
                .emailVerified(true)
                .googleSubject(googleProfile.subject())
                .name(displayName)
                .passwordHash(null)
                .role(role)
                .avatarUrl(googleProfile.picture())
                .creatorProgramStatus(role == UserRole.CREATOR ? CreatorProgramStatus.IN_PATH : CreatorProgramStatus.NONE)
                .active(true)
                .build();

        user = userRepository.save(user);

        if (role == UserRole.CREATOR) {
            creatorService.create(new CreatorCreateRequest(user.getId(), null, null, null, null, null, null));
        } else if (role == UserRole.BRAND) {
            brandService.create(new BrandCreateRequest(user.getId(), displayName, null, null, null));
        }

        return user;
    }

    private void requireActive(User user) {
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Account is disabled");
        }
    }

    private record OtpEntry(String otp, Instant expiresAt) {}
    public record AuthTokenPair(String accessToken, String refreshToken) {}
}
