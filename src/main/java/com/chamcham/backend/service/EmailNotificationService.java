package com.chamcham.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Stub email notification service.
 *
 * <p>Currently logs outbound emails to allow easy testing without an SMTP provider.
 * Replace the body of {@link #send} with your real SMTP / SES / Postmark client once
 * credentials are available. The method is already marked {@code @Async} so it never
 * blocks the request thread.</p>
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Async
    public void send(String toEmail, String toName, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) {
            log.debug("EmailNotificationService: skipping email — no address available");
            return;
        }
        // TODO: wire a real transactional email provider (e.g. AWS SES, Postmark, Mailgun).
        log.info("[EMAIL] to={} name={} subject='{}' body='{}'", toEmail, toName, subject, body);
    }
}

