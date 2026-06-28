package com.zingzing.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.commerce")
public class CommerceProperties {
    private int minimumCashAmountPkr = 500;

    public int getMinimumCashAmountPkr() { return minimumCashAmountPkr; }
    public void setMinimumCashAmountPkr(int minimumCashAmountPkr) { this.minimumCashAmountPkr = minimumCashAmountPkr; }
}
