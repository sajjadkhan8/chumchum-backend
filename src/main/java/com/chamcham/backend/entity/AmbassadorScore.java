package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.AmbassadorTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ambassador_scores")
public class AmbassadorScore {

    @Id
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private Creator creator;

    @Column(nullable = false)
    @Builder.Default
    private int total = 0;

    @Column(name = "delivery_score", nullable = false)
    @Builder.Default
    private int deliveryScore = 0;

    @Column(name = "account_age_score", nullable = false)
    @Builder.Default
    private int accountAgeScore = 0;

    @Column(name = "rating_score", nullable = false)
    @Builder.Default
    private int ratingScore = 0;

    @Column(name = "cancellation_score", nullable = false)
    @Builder.Default
    private int cancellationScore = 0;

    @Column(name = "profile_completeness_score", nullable = false)
    @Builder.Default
    private int profileCompletenessScore = 0;

    @Column(name = "consistency_score", nullable = false)
    @Builder.Default
    private int consistencyScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private AmbassadorTier tier = AmbassadorTier.RISING_CREATOR;

    @Column(name = "percentile_rank", nullable = false)
    @Builder.Default
    private int percentileRank = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> strengths = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> improvements = new ArrayList<>();

    @LastModifiedDate
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}

