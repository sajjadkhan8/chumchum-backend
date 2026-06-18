package com.zingzing.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auth_rate_limits", schema = "core",
        uniqueConstraints = @UniqueConstraint(columnNames = {"action", "identifier"}))
public class AuthRateLimit {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(nullable = false, length = 160)
    private String identifier;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "blocked_until")
    private Instant blockedUntil;
}
