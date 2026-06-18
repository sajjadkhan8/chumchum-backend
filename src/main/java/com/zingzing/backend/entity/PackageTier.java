package com.zingzing.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "package_tiers", schema = "core")
public class PackageTier {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private ServicePackage servicePackage;

    @Column(nullable = false, length = 50)
    private String name;  // e.g., "Lite", "Standard", "Premium" or "Basic", "Pro", "Elite"

    @Column(nullable = false)
    private Integer price;  // PKR amount in integer

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deliverables", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> deliverables = new ArrayList<>();  // Tier-specific deliverables

    @Column(name = "delivery_days")
    private Integer deliveryDays;  // Can override package delivery days

    @Builder.Default
    @Column(nullable = false)
    private Integer revisions = 1;

    @Column(columnDefinition = "text")
    private String description;  // Tier description, e.g., "Best for small campaigns"

    @Builder.Default
    @Column(nullable = false)
    private Integer position = 0;  // Order of tiers (0 = primary/base, 1 = first add-on, etc.)

    @Builder.Default
    @Column(nullable = false)
    private boolean isPrimary = false;  // Primary tier (v1: one primary + add-ons)

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}

