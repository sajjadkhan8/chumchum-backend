package com.zingzing.backend.entity;

import jakarta.persistence.*;
import lombok.*;
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
@Table(name = "package_analytics")
public class PackageAnalytics {

    @Id
    @Column(name = "package_id", nullable = false)
    private UUID packageId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id")
    private ServicePackage servicePackage;

    @Column(nullable = false)
    @Builder.Default
    private int views = 0;

    @Column(nullable = false)
    @Builder.Default
    private int clicks = 0;

    @Column(nullable = false)
    @Builder.Default
    private int inquiries = 0;

    @Column(name = "conversion_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal conversionRate = BigDecimal.ZERO;

    @Column(name = "completion_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal completionRate = BigDecimal.ZERO;

    @Column(name = "repeat_brands", nullable = false)
    @Builder.Default
    private int repeatBrands = 0;

    @Column(name = "engagement_performance", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal engagementPerformance = BigDecimal.ZERO;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

