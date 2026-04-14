package com.chamcham.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "creators", schema = "core")
public class Creator extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 1000)
    private String bio;

    @Column(length = 100)
    private String category;

    @Column(length = 255)
    private String tiktokUrl;

    @Column(length = 255)
    private String instagramUrl;

    @Column(length = 255)
    private String youtubeUrl;

    @Builder.Default
    @Column(nullable = false)
    private int followers = 0;

    @Builder.Default
    @Column(nullable = false)
    private int avgViews = 0;

    @Column(precision = 5, scale = 2)
    private BigDecimal engagementRate;

    @Builder.Default
    @Column(precision = 3, scale = 2, nullable = false)
    private BigDecimal rating = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private int totalReviews = 0;

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ServicePackage> packages = new ArrayList<>();
}
