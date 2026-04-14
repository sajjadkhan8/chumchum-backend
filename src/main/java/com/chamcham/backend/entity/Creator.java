package com.chamcham.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "creators", schema = "core")
@PrimaryKeyJoinColumn(name = "id")
public class Creator extends User {

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

    @Column(nullable = false)
    private int followers = 0;

    @Column(nullable = false)
    private int avgViews = 0;

    @Column(precision = 5, scale = 2)
    private BigDecimal engagementRate;

    @Column(precision = 3, scale = 2, nullable = false)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(nullable = false)
    private int totalReviews = 0;

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    private List<ServicePackage> packages = new ArrayList<>();
}
