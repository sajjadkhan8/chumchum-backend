package com.zingzing.backend.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zingzing.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GoogleTokenVerifierService {

    private static final Set<String> TRUSTED_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final Set<String> allowedClientIds;
    private final String tokenInfoUrl;
    private final RestClient restClient;

    public GoogleTokenVerifierService(
            @Value("${security.oauth.google.client-ids}") String clientIds,
            @Value("${security.oauth.google.token-info-url:https://oauth2.googleapis.com/tokeninfo}") String tokenInfoUrl,
            RestClient.Builder restClientBuilder
    ) {
        this.allowedClientIds = Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        if (this.allowedClientIds.isEmpty()) {
            throw new IllegalStateException("security.oauth.google.client-ids must include at least one client id");
        }
        this.tokenInfoUrl = tokenInfoUrl;
        this.restClient = restClientBuilder.build();
    }

    public VerifiedGoogleProfile verifyIdToken(String idToken) {
        GoogleTokenInfoResponse response;
        try {
            response = restClient.get()
                    .uri(tokenInfoUrl + "?id_token={idToken}", idToken)
                    .retrieve()
                    .body(GoogleTokenInfoResponse.class);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google token verification failed");
        }

        if (response == null || isBlank(response.sub()) || isBlank(response.email())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Google token payload");
        }

        if (!allowedClientIds.contains(response.aud())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google token audience mismatch");
        }

        if (!TRUSTED_ISSUERS.contains(response.iss())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Untrusted Google token issuer");
        }

        long expiryEpochSeconds;
        try {
            expiryEpochSeconds = Long.parseLong(response.exp());
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Google token expiry");
        }

        if (Instant.ofEpochSecond(expiryEpochSeconds).isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google token has expired");
        }

        if (!Boolean.parseBoolean(response.emailVerified())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google account email is not verified");
        }

        return new VerifiedGoogleProfile(response.sub(), response.email(), response.name(), response.picture());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record GoogleTokenInfoResponse(
            String iss,
            String aud,
            String sub,
            String email,
            @JsonProperty("email_verified")
            String emailVerified,
            String name,
            String picture,
            String exp
    ) {
    }

    public record VerifiedGoogleProfile(
            String subject,
            String email,
            String name,
            String picture
    ) {
    }
}


