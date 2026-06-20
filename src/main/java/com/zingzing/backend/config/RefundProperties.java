package com.zingzing.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.refunds")
public class RefundProperties {
    private String provider = "mock";
    private boolean allowMockInProduction = false;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public boolean isAllowMockInProduction() { return allowMockInProduction; }
    public void setAllowMockInProduction(boolean allowMockInProduction) { this.allowMockInProduction = allowMockInProduction; }
}
