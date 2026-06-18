package com.zingzing.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email notification service.
 *
 * <p>When {@code SMTP_HOST} is set and a {@link JavaMailSender} bean is available,
 * emails are dispatched via SMTP. Otherwise outbound emails are logged to the console
 * so the application remains fully functional without an SMTP provider.</p>
 *
 * <p>The method is marked {@code @Async} so it never blocks the request thread.</p>
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final boolean logContent;
    private final String smtpHost;
    private final String fromAddress;
    private final String fromName;

    public EmailNotificationService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${app.email.log-content:false}") boolean logContent,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.email.from-address:noreply@zingzing.pk}") String fromAddress,
            @Value("${app.email.from-name:ZingZing}") String fromName) {
        this.mailSender = mailSender;
        this.logContent = logContent;
        this.smtpHost = smtpHost;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Async
    public void send(String toEmail, String toName, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) {
            log.debug("EmailNotificationService: skipping email — no address available");
            return;
        }

        boolean smtpEnabled = smtpHost != null && !smtpHost.isBlank() && mailSender != null;

        if (!smtpEnabled) {
            log.info("[EMAIL] to={} name={} subject='{}'", toEmail, toName, subject);
            if (logContent) {
                log.info("[EMAIL CONTENT] {}", body);
            }
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            String fromPersonal = (fromName != null && !fromName.isBlank())
                    ? fromName + " <" + fromAddress + ">"
                    : fromAddress;
            helper.setFrom(fromAddress, fromName != null ? fromName : "");

            String toPersonal = (toName != null && !toName.isBlank())
                    ? toName + " <" + toEmail + ">"
                    : toEmail;
            helper.setTo(toEmail);

            helper.setSubject(subject);

            boolean isHtml = body != null && body.contains("<html");
            helper.setText(body != null ? body : "", isHtml);

            mailSender.send(message);
            log.info("[EMAIL SENT] to={} name={} subject='{}'", toEmail, toName, subject);
        } catch (MailException ex) {
            log.warn("[EMAIL FAILED] to={} subject='{}' error={}", toEmail, subject, ex.getMessage());
        } catch (Exception ex) {
            log.warn("[EMAIL FAILED] to={} subject='{}' error={}", toEmail, subject, ex.getMessage());
        }
    }
}
