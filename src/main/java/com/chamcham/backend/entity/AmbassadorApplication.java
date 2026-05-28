package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.AmbassadorAppStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ambassador_applications")
public class AmbassadorApplication {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false, unique = true)
    private Creator creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AmbassadorAppStatus status = AmbassadorAppStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "identity_verified", nullable = false)
    @Builder.Default
    private boolean identityVerified = false;

    @Column(name = "engagement_verified", nullable = false)
    @Builder.Default
    private boolean engagementVerified = false;

    @Column(name = "content_review_passed", nullable = false)
    @Builder.Default
    private boolean contentReviewPassed = false;

    @Column(name = "background_check_passed", nullable = false)
    @Builder.Default
    private boolean backgroundCheckPassed = false;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

