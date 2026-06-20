package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.creator.CreatorResponse;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.SocialAccount;
import com.zingzing.backend.entity.SocialOAuthState;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.entity.enums.VerificationSource;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.mapper.CreatorMapper;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.SocialOAuthStateRepository;
import com.zingzing.backend.repository.SocialAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/oauth")
public class SocialOAuthController {

    private final CreatorRepository creatorRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SocialOAuthStateRepository socialOAuthStateRepository;
    private final CreatorMapper creatorMapper;

    public SocialOAuthController(CreatorRepository creatorRepository,
                                 SocialAccountRepository socialAccountRepository,
                                 SocialOAuthStateRepository socialOAuthStateRepository,
                                 CreatorMapper creatorMapper) {
        this.creatorRepository = creatorRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.socialOAuthStateRepository = socialOAuthStateRepository;
        this.creatorMapper = creatorMapper;
    }

    public record OAuthCallbackRequest(@NotBlank String code, String state) {}

    @GetMapping("/{platform}/authorize")
    public ResponseEntity<Map<String, Object>> authorize(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable String platform,
            @RequestParam(name = "redirect_uri", required = false) String redirectUri
    ) {
        requireCreator(authUser);
        String normalized = normalizePlatform(platform);
        Creator creator = creatorRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
        String state = normalized + ":" + UUID.randomUUID();
        String target = redirectUri == null || redirectUri.isBlank()
                ? "/creator/social/oauth/callback"
                : redirectUri;
        socialOAuthStateRepository.save(SocialOAuthState.builder()
                .stateHash(hash(state))
                .creator(creator)
                .platform(normalized)
                .redirectUri(target)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build());
        String redirectUrl = UriComponentsBuilder.fromUriString(target)
                .queryParam("platform", normalized)
                .queryParam("code", "mock_" + normalized + "_" + UUID.randomUUID().toString().replace("-", ""))
                .queryParam("state", state)
                .build()
                .toUriString();
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("redirectUrl", redirectUrl)));
    }

    @PostMapping("/{platform}/callback")
    @Transactional
    public ResponseEntity<Map<String, Object>> callback(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable String platform,
            @Valid @RequestBody OAuthCallbackRequest request
    ) {
        requireCreator(authUser);
        String normalized = normalizePlatform(platform);
        SocialOAuthState oauthState = socialOAuthStateRepository.findById(hash(request.state()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "OAuth state is invalid or expired"));
        if (!oauthState.getCreator().getId().equals(authUser.userId()) || !oauthState.getPlatform().equals(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OAuth state does not match this connection");
        }
        if (oauthState.getUsedAt() != null || oauthState.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OAuth state is invalid or expired");
        }
        if (!request.code().startsWith("mock_") && request.code().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid OAuth authorization code");
        }
        Creator creator = creatorRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
        SocialAccount account = socialAccountRepository
                .findByCreatorIdAndPlatformIgnoreCase(creator.getId(), normalized)
                .orElseGet(() -> SocialAccount.builder()
                        .creator(creator)
                        .platform(normalized)
                        .externalId("mock_" + creator.getId())
                        .username(creator.getUsername() != null ? creator.getUsername() : normalized + "_creator")
                        .profileUrl("https://" + normalized + ".com/" + (creator.getUsername() != null ? creator.getUsername() : creator.getId()))
                        .followers(Math.max(creator.getFollowers(), 0))
                        .avgViews(creator.getAvgViews())
                        .engagementRate(creator.getEngagementRate() != null ? creator.getEngagementRate() : BigDecimal.ZERO)
                        .build());
        account.setExternalId("mock_" + creator.getId());
        account.setFollowers(Math.max(Math.max(account.getFollowers(), creator.getFollowers()), 0));
        account.setAvgViews(creator.getAvgViews());
        account.setEngagementRate(creator.getEngagementRate() != null ? creator.getEngagementRate() : BigDecimal.ZERO);
        account.setVerified(true);
        account.setVerifiedBy(VerificationSource.API_CONNECTED);
        account.setOauthStatus("CONNECTED");
        account.setLastSyncedAt(Instant.now());
        account.setSyncError(null);
        socialAccountRepository.save(account);
        oauthState.setUsedAt(Instant.now());
        socialOAuthStateRepository.save(oauthState);
        CreatorResponse response = creatorMapper.toResponse(creator);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    private void requireCreator(AuthenticatedUser authUser) {
        if (authUser == null || authUser.role() != UserRole.CREATOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can connect social accounts");
        }
    }

    private String normalizePlatform(String platform) {
        String normalized = platform == null ? "" : platform.trim().toLowerCase();
        if (normalized.isBlank() || normalized.length() > 30) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid social platform");
        }
        return normalized;
    }

    private String hash(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OAuth state is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not validate OAuth state");
        }
    }
}
