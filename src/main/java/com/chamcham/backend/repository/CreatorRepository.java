package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Creator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreatorRepository extends JpaRepository<Creator, UUID> {

    Optional<Creator> findByUsername(String username);

    List<Creator> findByIsTrendingTrue(Pageable pageable);

    List<Creator> findByAcceptsBarterTrue(Pageable pageable);

    List<Creator> findByIsFastResponderTrue(Pageable pageable);

    List<Creator> findByCityIgnoreCase(String city, Pageable pageable);

    Page<Creator> findByIsVerifiedTrue(Pageable pageable);

    @Query("select c from Creator c where c.city = :city and c.active = true")
    List<Creator> findByCityAndActiveTrue(@Param("city") String city, Pageable pageable);

    @Query("select c from Creator c where c.followers >= :min and c.followers <= :max and c.active = true")
    List<Creator> findByFollowersBetween(@Param("min") int min, @Param("max") int max, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO core.creators (
                id, bio, category, tiktok_url, instagram_url, youtube_url, facebook_url,
                followers, avg_views, engagement_rate, rating, total_reviews
            ) VALUES (
                :id, :bio, :category, :tiktokUrl, :instagramUrl, :youtubeUrl, :facebookUrl,
                :followers, :avgViews, :engagementRate, :rating, :totalReviews
            )
            """, nativeQuery = true)
    int insertProfile(
            @Param("id") UUID id,
            @Param("bio") String bio,
            @Param("category") String category,
            @Param("tiktokUrl") String tiktokUrl,
            @Param("instagramUrl") String instagramUrl,
            @Param("youtubeUrl") String youtubeUrl,
            @Param("facebookUrl") String facebookUrl,
            @Param("followers") int followers,
            @Param("avgViews") int avgViews,
            @Param("engagementRate") BigDecimal engagementRate,
            @Param("rating") BigDecimal rating,
            @Param("totalReviews") int totalReviews
    );
}
