package com.chamcham.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "social_accounts")
public class SocialAccount {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @Column(nullable = false, length = 30)
    private String platform;   // instagram | tiktok | youtube | facebook | snapchat

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "profile_url", length = 500)
    private String profileUrl;

    @Column(nullable = false)
    @Builder.Default
    private int followers = 0;

    @Column(name = "avg_views")
    private Integer avgViews;

    @Column(name = "engagement_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal engagementRate = BigDecimal.ZERO;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

