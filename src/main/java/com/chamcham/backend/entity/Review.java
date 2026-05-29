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
        uniqueConstraints = @UniqueConstraint(name = "uk_review_order", columnNames = {"order_id"}))
public class Review extends BaseEntity {

    @Id
    private UUID id;

    /** One review per order. */
    @OneToOne(fetch = FetchType.LAZY)
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
    private Package aPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    /** Legacy column – use rating field instead. */
    @Column
    private Integer star;

    @Column(length = 1000)
    private String description;
}
