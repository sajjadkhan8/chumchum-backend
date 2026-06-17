package com.chamcham.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BarterDeadlineScheduler {

    private final BarterDeadlineService barterDeadlineService;

    public BarterDeadlineScheduler(BarterDeadlineService barterDeadlineService) {
        this.barterDeadlineService = barterDeadlineService;
    }

    // Runs daily at 08:00 PKT (03:00 UTC)
    @Scheduled(cron = "0 0 3 * * *")
    public void checkBarterDeadlines() {
        barterDeadlineService.checkBarterDeadlines();
    }
}
