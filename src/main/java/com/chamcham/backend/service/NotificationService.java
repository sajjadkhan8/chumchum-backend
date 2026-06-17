package com.chamcham.backend.service;

import com.chamcham.backend.dto.notification.NotificationResponse;
import com.chamcham.backend.entity.Notification;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.NotificationRepository;
import com.chamcham.backend.repository.NotificationPreferenceRepository;
import com.chamcham.backend.repository.UserRepository;
import com.chamcham.backend.util.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public NotificationService(NotificationRepository notificationRepository,
                                UserRepository userRepository,
                                EmailNotificationService emailNotificationService,
                                NotificationPreferenceRepository notificationPreferenceRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    @Async
    public void sendMessageNotification(UUID userId, String title, String body, UUID conversationId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;
            var preference = notificationPreferenceRepository.findByUserId(userId).orElse(null);
            if (preference != null && !preference.isMessages()) return;

            notificationRepository.save(Notification.builder()
                    .user(user)
                    .type("message")
                    .title(title)
                    .body(body)
                    .entityType("conversation")
                    .entityId(conversationId)
                    .build());

            if (preference == null || preference.isEmailNotifications()) {
                emailNotificationService.send(user.getEmail(), user.getName(), title, body);
            }
        } catch (Exception ex) {
            log.error("Failed to send message notification for user {}: {}", userId, ex.getMessage(), ex);
        }
    }

    /**
     * Asynchronously creates an in-app notification and optionally sends an email.
     */
    @Async
    public void send(UUID userId, String type, String title, String body,
                     String entityType, UUID entityId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("NotificationService.send: user {} not found, skipping", userId);
                return;
            }

            Notification notification = Notification.builder()
                    .user(user)
                    .type(type)
                    .title(title)
                    .body(body)
                    .entityType(entityType)
                    .entityId(entityId)
                    .build();

            notificationRepository.save(notification);

            // Fire email in background (respects user preferences if configured)
            emailNotificationService.send(user.getEmail(), user.getName(), title, body);

        } catch (Exception ex) {
            log.error("Failed to persist notification for user {}: {}", userId, ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return PageResponse.from(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId);
    }

    @Transactional
    public void markOneRead(UUID notificationId, UUID userId) {
        int updated = notificationRepository.markOneRead(notificationId, userId);
        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Notification not found");
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getEntityType(),
                n.getEntityId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
