package com.zingzing.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    public SubscriptionScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    // Runs daily at 02:00 UTC to process due renewals
    @Scheduled(cron = "0 0 2 * * *")
    public void processRenewals() {
        subscriptionService.processRenewals();
    }
}
