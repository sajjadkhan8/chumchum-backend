package com.chamcham.backend.repository;

import com.chamcham.backend.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Object> {
    Optional<NotificationPreference> findByUserId(UUID userId);
}

