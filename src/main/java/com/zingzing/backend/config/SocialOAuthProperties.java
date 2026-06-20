package com.zingzing.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "social.oauth")
public class SocialOAuthProperties {
    private String mode = "mock";
    private boolean allowMockInProduction = false;
    private String environment = "development";
    private Map<String, Provider> providers = new HashMap<>();

    public boolean isMockMode() {
        return !"real".equalsIgnoreCase(mode);
    }

    public boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }

    public Provider provider(String platform) {
        return providers.get(platform == null ? "" : platform.toLowerCase());
    }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public boolean isAllowMockInProduction() { return allowMockInProduction; }
    public void setAllowMockInProduction(boolean allowMockInProduction) { this.allowMockInProduction = allowMockInProduction; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public Map<String, Provider> getProviders() { return providers; }
    public void setProviders(Map<String, Provider> providers) { this.providers = providers; }

    public static class Provider {
        private String clientId = "";
        private String clientSecret = "";
        private String scope = "";
        private String authorizationUrl = "";
        private String tokenUrl = "";
        private String profileUrl = "";

        public boolean isConfigured() {
            return notBlank(clientId) && notBlank(clientSecret) && notBlank(authorizationUrl)
                    && notBlank(tokenUrl) && notBlank(profileUrl);
        }

        private static boolean notBlank(String value) {
            return value != null && !value.isBlank();
        }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }

        public String getAuthorizationUrl() { return authorizationUrl; }
        public void setAuthorizationUrl(String authorizationUrl) { this.authorizationUrl = authorizationUrl; }

        public String getTokenUrl() { return tokenUrl; }
        public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }

        public String getProfileUrl() { return profileUrl; }
        public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    }
}
