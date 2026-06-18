package com.zingzing.backend.repository;

import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.enums.AvailabilityStatus;
import com.zingzing.backend.entity.enums.CreatorBadgeLevel;
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

    List<Creator> findByCityIgnoreCaseAndActiveTrue(String city, Pageable pageable);

    Page<Creator> findByIsVerifiedTrue(Pageable pageable);

    @Query("""
            select c from Creator c
            where (cast(:search as string) is null
                   or lower(c.name) like concat('%', lower(cast(:search as string)), '%')
                   or lower(c.email) like concat('%', lower(cast(:search as string)), '%')
                   or lower(c.username) like concat('%', lower(cast(:search as string)), '%'))
              and (:verified is null or c.isVerified = :verified)
            order by c.createdAt desc
            """)
    Page<Creator> searchForAdmin(@Param("search") String search,
                                 @Param("verified") Boolean verified,
                                 Pageable pageable);

    @Query("select c from Creator c where c.active = true order by c.engagementRate desc, c.rating desc")
    List<Creator> findRisingStars(Pageable pageable);

    @Query("select c from Creator c where c.active = true")
    List<Creator> findByCityAndActiveTrue(@Param("city") String city, Pageable pageable);

    @Query("select c from Creator c where c.followers >= :min and c.followers <= :max and c.active = true")
    List<Creator> findByFollowersBetween(@Param("min") int min, @Param("max") int max, Pageable pageable);

    /**
     * Explore-page filter – all params nullable; null means "no filter on that dimension".
     * Uses native PostgreSQL SQL for JSONB category matching via the @> operator.
     */
    @Query(value = """
            select distinct c.* from core.creators c
            join core.users u on u.id = c.id
            where u.is_active = true
              and (:search is null
                   or lower(u.name) like concat('%', lower(:search), '%')
                   or lower(c.username) like concat('%', lower(:search), '%')
                   or lower(c.bio) like concat('%', lower(:search), '%')
                   or lower(u.city) like concat('%', lower(:search), '%')
                   or lower(c.niche) like concat('%', lower(:search), '%')
                   or lower(c.preferred_industries) like concat('%', lower(:search), '%'))
              and (:city is null or lower(u.city) = lower(:city))
              and (:category is null
                   or lower(c.category) = lower(:category)
                   or c.categories @> jsonb_build_array(:category::text))
              and (:minFollowers is null or c.followers >= :minFollowers)
              and (:maxFollowers is null or c.followers <= :maxFollowers)
              and (:minRating is null or c.rating >= :minRating)
              and (:minPrice is null or c.min_price >= :minPrice)
              and (:maxPrice is null or c.max_price <= :maxPrice)
              and (:badgeLevel is null or c.badge_level = :badgeLevel)
              and (:availabilityStatus is null or c.availability_status = :availabilityStatus)
              and (:acceptsBarter is null or c.accepts_barter = :acceptsBarter)
              and (:isTrending is null or c.is_trending = :isTrending)
              and (:isFastResponder is null or c.is_fast_responder = :isFastResponder)
              and (:isVerified is null or c.is_verified = :isVerified)
              and (:minEngagementRate is null or c.engagement_rate >= :minEngagementRate)
              and (:platform is null or exists (
                   select 1 from core.social_accounts sa
                   where sa.creator_id = c.id and lower(sa.platform) = lower(:platform)))
            """,
            countQuery = """
            select count(distinct c.id) from core.creators c
            join core.users u on u.id = c.id
            where u.is_active = true
              and (:search is null
                   or lower(u.name) like concat('%', lower(:search), '%')
                   or lower(c.username) like concat('%', lower(:search), '%')
                   or lower(c.bio) like concat('%', lower(:search), '%')
                   or lower(u.city) like concat('%', lower(:search), '%')
                   or lower(c.niche) like concat('%', lower(:search), '%')
                   or lower(c.preferred_industries) like concat('%', lower(:search), '%'))
              and (:city is null or lower(u.city) = lower(:city))
              and (:category is null
                   or lower(c.category) = lower(:category)
                   or c.categories @> jsonb_build_array(:category::text))
              and (:minFollowers is null or c.followers >= :minFollowers)
              and (:maxFollowers is null or c.followers <= :maxFollowers)
              and (:minRating is null or c.rating >= :minRating)
              and (:minPrice is null or c.min_price >= :minPrice)
              and (:maxPrice is null or c.max_price <= :maxPrice)
              and (:badgeLevel is null or c.badge_level = :badgeLevel)
              and (:availabilityStatus is null or c.availability_status = :availabilityStatus)
              and (:acceptsBarter is null or c.accepts_barter = :acceptsBarter)
              and (:isTrending is null or c.is_trending = :isTrending)
              and (:isFastResponder is null or c.is_fast_responder = :isFastResponder)
              and (:isVerified is null or c.is_verified = :isVerified)
              and (:minEngagementRate is null or c.engagement_rate >= :minEngagementRate)
              and (:platform is null or exists (
                   select 1 from core.social_accounts sa
                   where sa.creator_id = c.id and lower(sa.platform) = lower(:platform)))
            """,
            nativeQuery = true)
    Page<Creator> search(
            @Param("search")             String search,
            @Param("city")               String city,
            @Param("category")           String category,
            @Param("minFollowers")       Integer minFollowers,
            @Param("maxFollowers")       Integer maxFollowers,
            @Param("minRating")          BigDecimal minRating,
            @Param("minPrice")           Integer minPrice,
            @Param("maxPrice")           Integer maxPrice,
            @Param("badgeLevel")         CreatorBadgeLevel badgeLevel,
            @Param("availabilityStatus") AvailabilityStatus availabilityStatus,
            @Param("acceptsBarter")      Boolean acceptsBarter,
            @Param("isTrending")         Boolean isTrending,
            @Param("isFastResponder")    Boolean isFastResponder,
            @Param("isVerified")         Boolean isVerified,
            @Param("minEngagementRate")  BigDecimal minEngagementRate,
            @Param("platform")           String platform,
            Pageable pageable
    );

    @Query("select c.id from Creator c")
    List<UUID> findAllIds();

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
