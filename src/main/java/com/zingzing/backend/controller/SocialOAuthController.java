package com.zingzing.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.zingzing.backend.config.SocialOAuthProperties;
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
import com.zingzing.backend.repository.SocialAccountRepository;
import com.zingzing.backend.repository.SocialOAuthStateRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
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
    private final SocialOAuthProperties oauthProperties;
    private final RestClient restClient;

    public SocialOAuthController(CreatorRepository creatorRepository,
                                 SocialAccountRepository socialAccountRepository,
                                 SocialOAuthStateRepository socialOAuthStateRepository,
                                 CreatorMapper creatorMapper,
                                 SocialOAuthProperties oauthProperties) {
        this.creatorRepository = creatorRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.socialOAuthStateRepository = socialOAuthStateRepository;
        this.creatorMapper = creatorMapper;
        this.oauthProperties = oauthProperties;
        this.restClient = RestClient.builder().build();
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
                : redirectUri.trim();
        socialOAuthStateRepository.save(SocialOAuthState.builder()
                .stateHash(hash(state))
                .creator(creator)
                .platform(normalized)
                .redirectUri(target)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build());
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("redirectUrl", buildRedirectUrl(normalized, target, state))));
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
        Creator creator = creatorRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
        if (oauthProperties.isMockMode()) {
            connectMockAccount(creator, normalized, request.code());
        } else {
            connectRealAccount(creator, normalized, request.code(), oauthState.getRedirectUri());
        }
        oauthState.setUsedAt(Instant.now());
        socialOAuthStateRepository.save(oauthState);
        CreatorResponse response = creatorMapper.toResponse(creator);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    private String buildRedirectUrl(String platform, String target, String state) {
        if (oauthProperties.isMockMode()) {
            if (oauthProperties.isProduction() && !oauthProperties.isAllowMockInProduction()) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Mock social OAuth is disabled in production");
            }
            return UriComponentsBuilder.fromUriString(target)
                    .queryParam("platform", platform)
                    .queryParam("code", "mock_" + platform + "_" + UUID.randomUUID().toString().replace("-", ""))
                    .queryParam("state", state)
                    .build()
                    .toUriString();
        }
        SocialOAuthProperties.Provider provider = configuredProvider(platform);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(provider.getAuthorizationUrl())
                .queryParam("client_id", provider.getClientId())
                .queryParam("redirect_uri", target)
                .queryParam("response_type", "code")
                .queryParam("state", state);
        if (provider.getScope() != null && !provider.getScope().isBlank()) {
            builder.queryParam("scope", provider.getScope());
        }
        return builder.build().toUriString();
    }

    private void connectMockAccount(Creator creator, String platform, String code) {
        if (!code.startsWith("mock_") && code.length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid OAuth authorization code");
        }
        SocialAccount account = socialAccountRepository
                .findByCreatorIdAndPlatformIgnoreCase(creator.getId(), platform)
                .orElseGet(() -> SocialAccount.builder()
                        .creator(creator)
                        .platform(platform)
                        .externalId("mock_" + creator.getId())
                        .username(creator.getUsername() != null ? creator.getUsername() : platform + "_creator")
                        .profileUrl("https://" + platform + ".com/" + (creator.getUsername() != null ? creator.getUsername() : creator.getId()))
                        .followers(Math.max(creator.getFollowers(), 0))
                        .avgViews(creator.getAvgViews())
                        .engagementRate(creator.getEngagementRate() != null ? creator.getEngagementRate() : BigDecimal.ZERO)
                        .build());
        applyAccount(account, "mock_" + creator.getId(),
                creator.getUsername() != null ? creator.getUsername() : platform + "_creator",
                "https://" + platform + ".com/" + (creator.getUsername() != null ? creator.getUsername() : creator.getId()),
                Math.max(Math.max(account.getFollowers(), creator.getFollowers()), 0),
                creator.getAvgViews(),
                creator.getEngagementRate() != null ? creator.getEngagementRate() : BigDecimal.ZERO,
                null);
    }

    private void connectRealAccount(Creator creator, String platform, String code, String redirectUri) {
        SocialOAuthProperties.Provider provider = configuredProvider(platform);
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("code", code);
            form.add("redirect_uri", redirectUri);
            form.add("client_id", provider.getClientId());
            form.add("client_secret", provider.getClientSecret());
            JsonNode tokenResponse = restClient.post()
                    .uri(provider.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            String accessToken = firstText(tokenResponse, "access_token", "data.access_token", "token.access_token");
            if (accessToken == null || accessToken.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "OAuth provider did not return an access token");
            }
            JsonNode profile = restClient.get()
                    .uri(provider.getProfileUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            String externalId = firstText(profile, "id", "data.id", "data.user.open_id", "data.user.id", "user.id", "account.id", "items.0.id");
            if (externalId == null || externalId.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "OAuth provider profile response did not include an id");
            }
            String username = firstText(profile, "username", "handle", "name", "display_name", "data.username", "data.name", "data.user.display_name", "snippet.title", "items.0.snippet.title");
            String profileUrl = firstText(profile, "profile_url", "profileUrl", "permalink", "data.profile_url", "data.user.profile_url", "data.user.avatar_url");
            Integer followers = firstInteger(profile, "followers", "followers_count", "subscriber_count", "data.followers_count", "data.user.follower_count", "statistics.subscriberCount", "items.0.statistics.subscriberCount");
            Integer avgViews = firstInteger(profile, "avg_views", "avgViews", "view_count", "statistics.viewCount", "items.0.statistics.viewCount");
            BigDecimal engagementRate = firstDecimal(profile, "engagement_rate", "engagementRate", "data.engagement_rate", "data.user.engagement_rate");
            SocialAccount account = socialAccountRepository
                    .findByCreatorIdAndPlatformIgnoreCase(creator.getId(), platform)
                    .orElseGet(() -> SocialAccount.builder().creator(creator).platform(platform).username(usernameOrFallback(username, creator, platform)).build());
            applyAccount(account, externalId, usernameOrFallback(username, creator, platform), profileUrl,
                    followers == null ? 0 : followers,
                    avgViews == null ? 0 : avgViews,
                    engagementRate == null ? BigDecimal.ZERO : engagementRate,
                    null);
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientException ex) {
            markOAuthError(creator, platform, "OAuth provider request failed: " + ex.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "OAuth provider request failed");
        }
    }

    private void applyAccount(SocialAccount account, String externalId, String username, String profileUrl,
                              int followers, int avgViews, BigDecimal engagementRate, String syncError) {
        account.setExternalId(externalId);
        account.setUsername(username);
        account.setProfileUrl(profileUrl);
        account.setFollowers(Math.max(followers, 0));
        account.setAvgViews(Math.max(avgViews, 0));
        account.setEngagementRate(engagementRate == null ? BigDecimal.ZERO : engagementRate);
        account.setVerified(true);
        account.setVerifiedBy(VerificationSource.API_CONNECTED);
        account.setOauthStatus(syncError == null ? "CONNECTED" : "ERROR");
        account.setLastSyncedAt(Instant.now());
        account.setSyncError(syncError);
        socialAccountRepository.save(account);
    }

    private void markOAuthError(Creator creator, String platform, String error) {
        SocialAccount account = socialAccountRepository.findByCreatorIdAndPlatformIgnoreCase(creator.getId(), platform)
                .orElseGet(() -> SocialAccount.builder()
                        .creator(creator)
                        .platform(platform)
                        .externalId("oauth_error_" + creator.getId())
                        .username(usernameOrFallback(null, creator, platform))
                        .build());
        applyAccount(account, account.getExternalId(), account.getUsername(), account.getProfileUrl(),
                account.getFollowers(), account.getAvgViews(), account.getEngagementRate(), error);
    }

    private SocialOAuthProperties.Provider configuredProvider(String platform) {
        SocialOAuthProperties.Provider provider = oauthProperties.provider(platform);
        if (provider == null || !provider.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Social OAuth provider is not configured for " + platform);
        }
        return provider;
    }

    private String usernameOrFallback(String username, Creator creator, String platform) {
        if (username != null && !username.isBlank()) return username;
        if (creator.getUsername() != null && !creator.getUsername().isBlank()) return creator.getUsername();
        return platform + "_creator";
    }

    private String firstText(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode value = atPath(node, path);
            if (value != null && !value.isNull() && value.isValueNode() && !value.asText().isBlank()) return value.asText();
        }
        return null;
    }

    private Integer firstInteger(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode value = atPath(node, path);
            if (value != null && value.isNumber()) return value.asInt();
            if (value != null && value.isTextual()) {
                try {
                    return Integer.parseInt(value.asText());
                } catch (NumberFormatException ignored) {
                    // Try the next path.
                }
            }
        }
        return null;
    }

    private BigDecimal firstDecimal(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode value = atPath(node, path);
            if (value != null && value.isNumber()) return value.decimalValue();
            if (value != null && value.isTextual()) {
                try {
                    return new BigDecimal(value.asText());
                } catch (NumberFormatException ignored) {
                    // Try the next path.
                }
            }
        }
        return null;
    }

    private JsonNode atPath(JsonNode node, String path) {
        if (node == null || path == null) return null;
        JsonNode cursor = node;
        for (String part : path.split("\\.")) {
            if (cursor == null || cursor.isNull()) return null;
            cursor = part.matches("\\d+") ? cursor.get(Integer.parseInt(part)) : cursor.get(part);
        }
        return cursor;
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
