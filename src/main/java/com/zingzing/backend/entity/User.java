package com.zingzing.backend.entity;

import com.zingzing.backend.entity.enums.CreatorProgramStatus;
import com.zingzing.backend.entity.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", schema = "core")
@Inheritance(strategy = InheritanceType.JOINED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "google_subject", unique = true, length = 128)
    private String googleSubject;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String image;

    @Column(length = 80)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    @Builder.Default
    private CreatorProgramStatus creatorProgramStatus = CreatorProgramStatus.NONE;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "ban_reason", length = 500)
    private String banReason;

    @Column(name = "suspended_until")
    private OffsetDateTime suspendedUntil;

    // CRIT-7: GDPR Article 7 consent — timestamp and version stored at registration
    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "terms_version", length = 20)
    private String termsVersion;

    // HIGH-8: TOTP MFA fields — admin accounts only
    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (creatorProgramStatus == null) {
            creatorProgramStatus = CreatorProgramStatus.NONE;
        }
    }

}
