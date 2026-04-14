package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Creator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreatorRepository extends JpaRepository<Creator, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            insert into core.creators (
                id,
                bio,
                category,
                tiktok_url,
                instagram_url,
                youtube_url,
                followers,
                avg_views,
                engagement_rate,
                rating,
                total_reviews
            ) values (
                :id,
                :bio,
                :category,
                :tiktokUrl,
                :instagramUrl,
                :youtubeUrl,
                :followers,
                :avgViews,
                :engagementRate,
                :rating,
                :totalReviews
            )
            """, nativeQuery = true)
    int insertProfile(
            @Param("id") UUID id,
            @Param("bio") String bio,
            @Param("category") String category,
            @Param("tiktokUrl") String tiktokUrl,
            @Param("instagramUrl") String instagramUrl,
            @Param("youtubeUrl") String youtubeUrl,
            @Param("followers") int followers,
            @Param("avgViews") int avgViews,
            @Param("engagementRate") BigDecimal engagementRate,
            @Param("rating") BigDecimal rating,
            @Param("totalReviews") int totalReviews
    );
}
