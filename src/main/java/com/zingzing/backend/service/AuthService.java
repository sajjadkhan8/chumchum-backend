package com.zingzing.backend.service;

import com.zingzing.backend.config.security.JwtService;
import com.zingzing.backend.dto.auth.*;
import com.zingzing.backend.dto.auth.*;
import com.zingzing.backend.dto.brand.BrandCreateRequest;
import com.zingzing.backend.dto.creator.CreatorCreateRequest;
import com.zingzing.backend.dto.user.UserResponse;
import com.zingzing.backend.entity.*;
import com.zingzing.backend.entity.AuthOtpChallenge;
import com.zingzing.backend.entity.AuthPasswordResetToken;
import com.zingzing.backend.entity.AuthRefreshToken;
import com.zingzing.backend.entity.User;
import com.zingzing.backend.entity.enums.CreatorProgramStatus;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.mapper.UserMapper;
import com.zingzing.backend.repository.*;
import com.zingzing.backend.repository.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
    private final TotpService totpService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshExpirationSeconds;
    private final long adminRefreshExpirationSeconds;
    private final String frontendBaseUrl;

    static final String CURRENT_TERMS_VERSION = "1.0";

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
            TotpService totpService,
            @Value("${security.refresh.expiration-seconds:2592000}") long refreshExpirationSeconds,
            @Value("${security.refresh.admin-expiration-seconds:86400}") long adminRefreshExpirationSeconds,
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
        this.totpService = totpService;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
        this.adminRefreshExpirationSeconds = adminRefreshExpirationSeconds;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public AuthTokenResponse register(AuthRegisterRequest request) {
        if (request.role().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot register with this role");
        }
        if (!Boolean.TRUE.equals(request.termsAccepted())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You must accept the Terms of Service to create an account");
        }
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
                .termsAcceptedAt(Instant.now())
                .termsVersion(CURRENT_TERMS_VERSION)
                .build();
        user = userRepository.save(user);

        if (request.role() == UserRole.CREATOR) {
            creatorService.create(new CreatorCreateRequest(user.getId(), null, null, null, null, null));
            creatorRepository.findById(user.getId())
                    .ifPresent(creator -> affiliateService.recordCreatorSignupAttribution(creator, request.affiliateCode()));
        } else if (request.role() == UserRole.BRAND) {
            brandService.create(new BrandCreateRequest(user.getId(), request.name(), null, null, null));
        }

        clearRateLimit("register", email);
        return issueTokens(user);
    }

    @Transactional
    public LoginResult login(AuthLoginRequest request) {
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

        // HIGH-8: Admin accounts with MFA enabled require a TOTP step before issuing tokens
        if (user.getRole().isAdmin() && user.isMfaEnabled()) {
            String challengeToken = jwtService.generateShortLivedToken(user.getId(), "mfa_challenge", 300);
            return new LoginResult(null, challengeToken);
        }
        return new LoginResult(issueTokens(user), null);
    }

    /** HIGH-8: Second factor of admin MFA login — verifies TOTP code and issues full tokens. */
    @Transactional
    public AuthTokenResponse verifyMfaChallenge(String challengeToken, String totpCode) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(challengeToken);
        } catch (JwtException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA challenge token is expired or invalid");
        }
        if (!"mfa_challenge".equals(claims.get("type", String.class))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid challenge token");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        requireActive(user);
        if (!totpService.verify(user.getTotpSecret(), totpCode)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid TOTP code");
        }
        return issueTokens(user);
    }

    /** HIGH-8: Generates a TOTP secret and OTP-Auth URI for an admin's authenticator app. */
    @Transactional
    public MfaSetupResponse setupMfa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.getRole().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MFA is only available for admin accounts");
        }
        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        user.setMfaEnabled(false); // not active until verified with a real code
        userRepository.save(user);
        return new MfaSetupResponse(secret, totpService.buildOtpAuthUri(secret, user.getEmail()));
    }

    /** HIGH-8: Confirms setup by verifying a TOTP code, then enables MFA on the account. */
    @Transactional
    public void enableMfa(UUID userId, String totpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getTotpSecret() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Call /auth/admin/mfa/setup before enabling MFA");
        }
        if (!totpService.verify(user.getTotpSecret(), totpCode)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid TOTP code — scan the QR again and retry");
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    /** HIGH-8: Disables MFA after confirming identity with a current TOTP code. */
    @Transactional
    public void disableMfa(UUID userId, String totpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isMfaEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MFA is not currently enabled");
        }
        if (!totpService.verify(user.getTotpSecret(), totpCode)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid TOTP code");
        }
        user.setMfaEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
    }

    @Transactional
    public AuthSendOtpResponse sendOtp(AuthSendOtpRequest request) {
        enforceRateLimit("send_otp", request.phone(), 3, 10, 30);
        // Per-send cooldown: reject if an OTP was sent within the last 60 seconds
        otpChallengeRepository.findById(request.phone()).ifPresent(existing -> {
            if (existing.getSentAt().plusSeconds(60).isAfter(Instant.now())) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another OTP");
            }
        });
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
        if (challenge.getExpiresAt().isBefore(Instant.now()) || challenge.getAttempts() >= 5
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
        String tokenHash = hash(request.refreshToken());
        // Rate limit per token to prevent flooding a single stolen token
        enforceRateLimit("refresh", tokenHash.substring(0, 16), 10, 1, 5);

        AuthRefreshToken stored = refreshTokenRepository.findById(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));

        // Reuse detection: a previously-rotated token being presented again indicates potential theft.
        // Revoke all sessions for this user as a precaution.
        if (stored.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllByUserId(stored.getUser().getId(), Instant.now());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        User user = stored.getUser();
        requireActive(user);

        // Rotate: revoke old token, issue a new one
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        String newRefreshToken = randomToken();
        long ttl = user.getRole().isAdmin() ? adminRefreshExpirationSeconds : refreshExpirationSeconds;
        refreshTokenRepository.save(AuthRefreshToken.builder()
                .tokenHash(hash(newRefreshToken))
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(ttl))
                .build());

        return new AuthTokenPair(jwtService.generateToken(user.getId(), user.getRole()), newRefreshToken);
    }

    @Transactional
    public AuthTokenResponse authenticateWithGoogle(AuthGoogleRequest request) {
        if (request.role() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role is required for Google authentication");
        }
        if (request.role().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot register with this role");
        }

        GoogleTokenVerifierService.VerifiedGoogleProfile googleProfile = googleTokenVerifierService.verifyIdToken(request.idToken());
        boolean[] createdUser = {false};
        User user = userRepository.findByGoogleSubject(googleProfile.subject())
                .map(existing -> updateLinkedGoogleUser(existing, googleProfile, request))
                .orElseGet(() -> userRepository.findByEmail(googleProfile.email())
                        .map(existing -> linkGoogleToExistingEmail(existing, googleProfile, request))
                        .orElseGet(() -> {
                            if (!Boolean.TRUE.equals(request.termsAccepted())) {
                                throw new ApiException(HttpStatus.BAD_REQUEST,
                                        "You must accept the Terms of Service to create an account");
                            }
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
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String tokenHash = hash(refreshToken);
        enforceRateLimit("logout", tokenHash.substring(0, 16), 5, 1, 5);
        refreshTokenRepository.findById(tokenHash).ifPresent(token -> {
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
        long ttl = user.getRole().isAdmin() ? adminRefreshExpirationSeconds : refreshExpirationSeconds;
        refreshTokenRepository.save(AuthRefreshToken.builder()
                .tokenHash(hash(refreshToken))
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(ttl))
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
                .termsAcceptedAt(Instant.now())
                .termsVersion(CURRENT_TERMS_VERSION)
                .build());
        if (role == UserRole.CREATOR) creatorService.create(new CreatorCreateRequest(user.getId(), null, null, null, null, null));
        if (role == UserRole.BRAND) brandService.create(new BrandCreateRequest(user.getId(), displayName, null, null, null));
        return user;
    }

    private void requireActive(User user) {
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Account is disabled");
        }
    }

    public record AuthTokenPair(String accessToken, String refreshToken) {}

    /** Result of a login attempt. Exactly one field is non-null. */
    public record LoginResult(AuthTokenResponse tokens, String mfaChallengeToken) {
        public boolean mfaRequired() { return mfaChallengeToken != null; }
    }

    /** MFA setup response: raw secret for manual entry + OTP-Auth URI for QR rendering. */
    public record MfaSetupResponse(String secret, String otpAuthUri) {}
}
