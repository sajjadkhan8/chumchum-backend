package com.chamcham.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_order_type", columnNames = {"order_id", "reviewer_type"}))
public class Review extends BaseEntity {

    @Id
    private UUID id;

    public enum ReviewerType { BRAND, CREATOR }

    @Enumerated(EnumType.STRING)
    @Column(name = "reviewer_type", nullable = false, length = 10)
    @Builder.Default
    private ReviewerType reviewerType = ReviewerType.BRAND;

    /** One review per (order, reviewer_type). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    /** 1–5 rating (was "star" in legacy schema). */
    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "text")
    private String comment;

    // -------- legacy columns kept for non-null DB compat --------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private ServicePackage servicePackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    /** Legacy column – use rating field instead. */
    @Column
    private Integer star;

    @Column(length = 1000)
    private String description;
}
