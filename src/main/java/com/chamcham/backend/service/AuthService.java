package com.chamcham.backend.service;

import com.chamcham.backend.config.security.JwtService;
import com.chamcham.backend.dto.auth.*;
import com.chamcham.backend.dto.brand.BrandCreateRequest;
import com.chamcham.backend.dto.creator.CreatorCreateRequest;
import com.chamcham.backend.dto.user.UserResponse;
import com.chamcham.backend.entity.*;
import com.chamcham.backend.entity.enums.CreatorProgramStatus;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.UserMapper;
import com.chamcham.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CreatorService creatorService;
    private final BrandService brandService;
    private final CreatorRepository creatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final EmailNotificationService emailNotificationService;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final AuthPasswordResetTokenRepository resetTokenRepository;
    private final AuthOtpChallengeRepository otpChallengeRepository;
    private final AuthRateLimitService rateLimitService;
    private final AffiliateService affiliateService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshExpirationSeconds;
    private final String frontendBaseUrl;

    public AuthService(
            UserRepository userRepository,
            UserMapper userMapper,
            CreatorService creatorService,
            BrandService brandService,
            CreatorRepository creatorRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleTokenVerifierService googleTokenVerifierService,
            EmailNotificationService emailNotificationService,
            AuthRefreshTokenRepository refreshTokenRepository,
            AuthPasswordResetTokenRepository resetTokenRepository,
            AuthOtpChallengeRepository otpChallengeRepository,
            AuthRateLimitService rateLimitService,
            AffiliateService affiliateService,
            @Value("${security.refresh.expiration-seconds:2592000}") long refreshExpirationSeconds,
            @Value("${app.frontend-base-url:http://localhost:3000}") String frontendBaseUrl
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.creatorService = creatorService;
        this.brandService = brandService;
        this.creatorRepository = creatorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.emailNotificationService = emailNotificationService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.otpChallengeRepository = otpChallengeRepository;
        this.rateLimitService = rateLimitService;
        this.affiliateService = affiliateService;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public AuthTokenResponse register(AuthRegisterRequest request) {
        String email = normalizeIdentifier(request.email());
        enforceRateLimit("register", email, 5, 30, 60);
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = User.builder()
                .username(generateUniqueUsername(email))
                .email(email)
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
            creatorRepository.findById(user.getId())
                    .ifPresent(creator -> affiliateService.recordCreatorSignupAttribution(creator, request.affiliateCode()));
        } else if (request.role() == UserRole.BRAND) {
            brandService.create(new BrandCreateRequest(user.getId(), request.name(), null, null, null));
        }

        clearRateLimit("register", email);
        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResponse login(AuthLoginRequest request) {
        String email = normalizeIdentifier(request.email());
        enforceRateLimit("login", email, 8, 15, 30);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        requireActive(user);
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "This account uses social login. Continue with Google.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        clearRateLimit("login", email);
        return issueTokens(user);
    }

    @Transactional
    public AuthSendOtpResponse sendOtp(AuthSendOtpRequest request) {
        enforceRateLimit("send_otp", request.phone(), 3, 10, 30);
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        otpChallengeRepository.save(AuthOtpChallenge.builder()
                .phone(request.phone())
                .otpHash(hash(otp))
                .attempts(0)
                .sentAt(Instant.now())
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build());
        return new AuthSendOtpResponse("OTP sent successfully", 300);
    }

    @Transactional
    public AuthTokenResponse verifyOtp(AuthVerifyOtpRequest request) {
        enforceRateLimit("verify_otp", request.phone(), 6, 10, 30);
        AuthOtpChallenge challenge = otpChallengeRepository.findById(request.phone())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP"));

        challenge.setAttempts(challenge.getAttempts() + 1);
        otpChallengeRepository.save(challenge);
        if (challenge.getExpiresAt().isBefore(Instant.now()) || challenge.getAttempts() > 5
                || !challenge.getOtpHash().equals(hash(request.otp()))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }

        User user = userRepository.findByPhone(request.phone()).map(existing -> {
            requireActive(existing);
            return existing;
        }).orElseGet(() -> userRepository.save(User.builder()
                .username(generateUniqueUsername(request.phone()))
                .phone(request.phone())
                .name(request.phone())
                .email(request.phone().replace("+", "") + "@phone.zingzing.pk")
                .role(UserRole.CREATOR)
                .creatorProgramStatus(CreatorProgramStatus.IN_PATH)
                .active(true)
                .build()));

        otpChallengeRepository.delete(challenge);
        clearRateLimit("verify_otp", request.phone());
        return issueTokens(user);
    }

    @Transactional
    public AuthTokenPair refresh(AuthRefreshRequest request) {
        AuthRefreshToken stored = refreshTokenRepository.findById(hash(request.refreshToken()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));
        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        User user = stored.getUser();
        requireActive(user);
        return new AuthTokenPair(jwtService.generateToken(user.getId(), user.getRole()), request.refreshToken());
    }

    @Transactional
    public AuthTokenResponse authenticateWithGoogle(AuthGoogleRequest request) {
        if (request.role() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role is required for Google authentication");
        }

        GoogleTokenVerifierService.VerifiedGoogleProfile googleProfile = googleTokenVerifierService.verifyIdToken(request.idToken());
        boolean[] createdUser = {false};
        User user = userRepository.findByGoogleSubject(googleProfile.subject())
                .map(existing -> updateLinkedGoogleUser(existing, googleProfile, request))
                .orElseGet(() -> userRepository.findByEmail(googleProfile.email())
                        .map(existing -> linkGoogleToExistingEmail(existing, googleProfile, request))
                        .orElseGet(() -> {
                            createdUser[0] = true;
                            return createGoogleUser(googleProfile, request);
                        }));

        requireActive(user);
        if (createdUser[0] && user.getRole() == UserRole.CREATOR) {
            creatorRepository.findById(user.getId())
                    .ifPresent(creator -> affiliateService.recordCreatorSignupAttribution(creator, request.affiliateCode()));
        }
        return issueTokens(user);
    }

    @Transactional
    public void forgotPassword(AuthForgotPasswordRequest request) {
        String email = normalizeIdentifier(request.email());
        enforceRateLimit("forgot_password", email, 3, 15, 60);
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = randomToken();
            resetTokenRepository.save(AuthPasswordResetToken.builder()
                    .tokenHash(hash(rawToken))
                    .user(user)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                    .build());
            emailNotificationService.send(user.getEmail(), user.getName(), "Reset your ZingZing password",
                    frontendBaseUrl + "/forgot-password?token=" + rawToken);
        });
    }

    @Transactional
    public void resetPassword(AuthResetPasswordRequest request) {
        AuthPasswordResetToken token = resetTokenRepository.findById(hash(request.token()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid/expired reset token"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid/expired reset token");
        }
        User user = token.getUser();
        requireActive(user);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        token.setUsedAt(Instant.now());
        resetTokenRepository.save(token);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        refreshTokenRepository.findById(hash(refreshToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    public UserResponse me(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        requireActive(user);
        return userMapper.toResponse(user);
    }

    private AuthTokenResponse issueTokens(User user) {
        requireActive(user);
        String accessToken = jwtService.generateToken(user.getId(), user.getRole());
        String refreshToken = randomToken();
        refreshTokenRepository.save(AuthRefreshToken.builder()
                .tokenHash(hash(refreshToken))
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(refreshExpirationSeconds))
                .build());
        return new AuthTokenResponse(accessToken, refreshToken, userMapper.toResponse(user));
    }

    private void enforceRateLimit(String action, String identifier, int maxAttempts, long windowMinutes, long blockMinutes) {
        if (rateLimitService.recordAndCheck(action, identifier, maxAttempts, windowMinutes, blockMinutes)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again later.");
        }
    }

    private void clearRateLimit(String action, String identifier) {
        rateLimitService.clear(action, identifier);
    }

    private String normalizeIdentifier(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String randomToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String generateUsername(String seed) {
        String normalized = seed.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (normalized.length() < 3) normalized = "user" + normalized;
        return normalized.length() > 40 ? normalized.substring(0, 40) : normalized;
    }

    private String generateUniqueUsername(String seed) {
        String base = generateUsername(seed);
        if (!userRepository.existsByUsername(base)) return base;
        for (int i = 1; i <= 9999; i++) {
            String suffix = String.valueOf(i);
            String candidate = base.substring(0, Math.min(base.length(), 40 - suffix.length())) + suffix;
            if (!userRepository.existsByUsername(candidate)) return candidate;
        }
        return base.substring(0, Math.min(base.length(), 32)) + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private User updateLinkedGoogleUser(User existing, GoogleTokenVerifierService.VerifiedGoogleProfile profile, AuthGoogleRequest request) {
        requireActive(existing);
        if (request.role() != existing.getRole()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This Google account is already linked to another role");
        }
        if (existing.getEmail() == null || existing.getEmail().isBlank()) existing.setEmail(profile.email());
        existing.setEmailVerified(true);
        if ((existing.getName() == null || existing.getName().isBlank()) && profile.name() != null) existing.setName(profile.name());
        if ((existing.getAvatarUrl() == null || existing.getAvatarUrl().isBlank()) && profile.picture() != null) existing.setAvatarUrl(profile.picture());
        return userRepository.save(existing);
    }

    private User linkGoogleToExistingEmail(User existing, GoogleTokenVerifierService.VerifiedGoogleProfile profile, AuthGoogleRequest request) {
        requireActive(existing);
        if (request.role() != existing.getRole()) throw new ApiException(HttpStatus.BAD_REQUEST, "Email already exists with another role");
        if (existing.getGoogleSubject() != null && !existing.getGoogleSubject().equals(profile.subject())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already linked with another Google account");
        }
        existing.setGoogleSubject(profile.subject());
        existing.setEmailVerified(true);
        if ((existing.getName() == null || existing.getName().isBlank()) && profile.name() != null) existing.setName(profile.name());
        if ((existing.getAvatarUrl() == null || existing.getAvatarUrl().isBlank()) && profile.picture() != null) existing.setAvatarUrl(profile.picture());
        return userRepository.save(existing);
    }

    private User createGoogleUser(GoogleTokenVerifierService.VerifiedGoogleProfile profile, AuthGoogleRequest request) {
        UserRole role = request.role();
        String displayName = request.name() != null && !request.name().isBlank() ? request.name().trim()
                : (profile.name() != null && !profile.name().isBlank() ? profile.name().trim() : profile.email());
        User user = userRepository.save(User.builder()
                .username(generateUniqueUsername(profile.email()))
                .email(profile.email())
                .emailVerified(true)
                .googleSubject(profile.subject())
                .name(displayName)
                .role(role)
                .avatarUrl(profile.picture())
                .creatorProgramStatus(role == UserRole.CREATOR ? CreatorProgramStatus.IN_PATH : CreatorProgramStatus.NONE)
                .active(true)
                .build());
        if (role == UserRole.CREATOR) creatorService.create(new CreatorCreateRequest(user.getId(), null, null, null, null, null, null));
        if (role == UserRole.BRAND) brandService.create(new BrandCreateRequest(user.getId(), displayName, null, null, null));
        return user;
    }

    private void requireActive(User user) {
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Account is disabled");
        }
    }

    public record AuthTokenPair(String accessToken, String refreshToken) {}
}
