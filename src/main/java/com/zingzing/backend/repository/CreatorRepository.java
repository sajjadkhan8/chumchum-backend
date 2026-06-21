package com.zingzing.backend.repository;

import com.zingzing.backend.entity.Creator;
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
            select u.*,
                   c.bio, c.cover_image_url, c.website, c.availability_status,
                   c.is_filer, c.response_time, c.min_price, c.max_price,
                   c.is_verified, c.verification_status, c.badge_level, c.is_trending, c.is_fast_responder,
                   c.completed_deals, c.accepts_barter, c.accepts_hybrid_deals,
                   c.minimum_budget, c.languages, c.categories, c.deal_types, c.barter_types,
                   c.followers, c.avg_views, c.engagement_rate, c.rating, c.total_reviews,
                   c.rate_card_reel, c.rate_card_story, c.rate_card_post, c.rate_card_video
            from core.creators c
            join core.users u on u.id = c.id
            where u.is_active = true
              and (:search is null
                   or lower(u.name) like concat('%', lower(:search), '%')
                   or lower(u.username) like concat('%', lower(:search), '%')
                   or lower(c.bio) like concat('%', lower(:search), '%')
                   or lower(u.city) like concat('%', lower(:search), '%'))
              and (:cities is null or lower(u.city) in (
                   select lower(v) from jsonb_array_elements_text((:cities)::jsonb) v))
              and (:categories is null or exists (
                   select 1 from jsonb_array_elements_text((:categories)::jsonb) v
                   where c.categories @> jsonb_build_array(v)))
              and (:languages is null or exists (
                   select 1 from jsonb_array_elements_text((:languages)::jsonb) v
                   where c.languages @> jsonb_build_array(v)))
              and (:minFollowers is null or c.followers >= :minFollowers)
              and (:maxFollowers is null or c.followers <= :maxFollowers)
              and (:minRating is null or c.rating >= :minRating)
              and (:minPrice is null or c.min_price >= :minPrice)
              and (:maxPrice is null or c.max_price <= :maxPrice)
              and (:minReviews is null or c.total_reviews >= :minReviews)
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
              and (:minCompletionRate is null or (
                   select case
                            when count(*) = 0 then 0
                            else round(sum(case when o.status = 'COMPLETED' then 1.0 else 0.0 end) * 100 / count(*))
                          end
                   from core.orders o
                   where o.creator_id = c.id) >= :minCompletionRate)
              and (:maxRateCardReel is null or c.rate_card_reel is null or c.rate_card_reel <= :maxRateCardReel)
              and (:maxRateCardStory is null or c.rate_card_story is null or c.rate_card_story <= :maxRateCardStory)
              and (:maxRateCardPost is null or c.rate_card_post is null or c.rate_card_post <= :maxRateCardPost)
              and (:maxRateCardVideo is null or c.rate_card_video is null or c.rate_card_video <= :maxRateCardVideo)
            order by
              case when :sortBy = 'trending' then c.is_trending end desc,
              case when :sortBy = 'top_rated' then c.rating end desc nulls last,
              case when :sortBy = 'budget_friendly' then coalesce(c.min_price, 2147483647) end asc,
              case when :sortBy = 'budget_high' then coalesce(c.max_price, c.min_price, 0) end desc,
              case when :sortBy = 'created_at' then u.created_at end desc nulls last,
              u.created_at desc
            """,
            countQuery = """
            select count(c.id) from core.creators c
            join core.users u on u.id = c.id
            where u.is_active = true
              and (:search is null
                   or lower(u.name) like concat('%', lower(:search), '%')
                   or lower(u.username) like concat('%', lower(:search), '%')
                   or lower(c.bio) like concat('%', lower(:search), '%')
                   or lower(u.city) like concat('%', lower(:search), '%'))
              and (:cities is null or lower(u.city) in (
                   select lower(v) from jsonb_array_elements_text((:cities)::jsonb) v))
              and (:categories is null or exists (
                   select 1 from jsonb_array_elements_text((:categories)::jsonb) v
                   where c.categories @> jsonb_build_array(v)))
              and (:languages is null or exists (
                   select 1 from jsonb_array_elements_text((:languages)::jsonb) v
                   where c.languages @> jsonb_build_array(v)))
              and (:minFollowers is null or c.followers >= :minFollowers)
              and (:maxFollowers is null or c.followers <= :maxFollowers)
              and (:minRating is null or c.rating >= :minRating)
              and (:minPrice is null or c.min_price >= :minPrice)
              and (:maxPrice is null or c.max_price <= :maxPrice)
              and (:minReviews is null or c.total_reviews >= :minReviews)
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
              and (:minCompletionRate is null or (
                   select case
                            when count(*) = 0 then 0
                            else round(sum(case when o.status = 'COMPLETED' then 1.0 else 0.0 end) * 100 / count(*))
                          end
                   from core.orders o
                   where o.creator_id = c.id) >= :minCompletionRate)
              and (:maxRateCardReel is null or c.rate_card_reel is null or c.rate_card_reel <= :maxRateCardReel)
              and (:maxRateCardStory is null or c.rate_card_story is null or c.rate_card_story <= :maxRateCardStory)
              and (:maxRateCardPost is null or c.rate_card_post is null or c.rate_card_post <= :maxRateCardPost)
              and (:maxRateCardVideo is null or c.rate_card_video is null or c.rate_card_video <= :maxRateCardVideo)
            """,
            nativeQuery = true)
    Page<Creator> search(
            @Param("search")             String search,
            @Param("cities")             String cities,
            @Param("categories")         String categories,
            @Param("languages")          String languages,
            @Param("minFollowers")       Integer minFollowers,
            @Param("maxFollowers")       Integer maxFollowers,
            @Param("minRating")          BigDecimal minRating,
            @Param("minPrice")           Integer minPrice,
            @Param("maxPrice")           Integer maxPrice,
            @Param("minReviews")         Integer minReviews,
            @Param("badgeLevel")         String badgeLevel,
            @Param("availabilityStatus") String availabilityStatus,
            @Param("acceptsBarter")      Boolean acceptsBarter,
            @Param("isTrending")         Boolean isTrending,
            @Param("isFastResponder")    Boolean isFastResponder,
            @Param("isVerified")         Boolean isVerified,
            @Param("minEngagementRate")  BigDecimal minEngagementRate,
            @Param("platform")           String platform,
            @Param("minCompletionRate")  Integer minCompletionRate,
            @Param("maxRateCardReel")    Integer maxRateCardReel,
            @Param("maxRateCardStory")   Integer maxRateCardStory,
            @Param("maxRateCardPost")    Integer maxRateCardPost,
            @Param("maxRateCardVideo")   Integer maxRateCardVideo,
            @Param("sortBy")             String sortBy,
            Pageable pageable
    );

    @Query("select c.id from Creator c")
    List<UUID> findAllIds();

    @Query(value = """
            select distinct t.elem
            from core.creators c
            join core.users u on u.id = c.id,
            lateral jsonb_array_elements_text(c.categories) as t(elem)
            where u.is_active = true
              and c.categories is not null
              and jsonb_array_length(c.categories) > 0
            order by t.elem
            """, nativeQuery = true)
    List<String> findDistinctCategories();

    @Query(value = """
            select distinct t.elem
            from core.creators c
            join core.users u on u.id = c.id,
            lateral jsonb_array_elements_text(c.languages) as t(elem)
            where u.is_active = true
              and c.languages is not null
              and jsonb_array_length(c.languages) > 0
            order by t.elem
            """, nativeQuery = true)
    List<String> findDistinctLanguages();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO core.creators (
                id, bio,
                followers, avg_views, engagement_rate, rating, total_reviews
            ) VALUES (
                :id, :bio,
                :followers, :avgViews, :engagementRate, :rating, :totalReviews
            )
            """, nativeQuery = true)
    int insertProfile(
            @Param("id") UUID id,
            @Param("bio") String bio,
            @Param("followers") int followers,
            @Param("avgViews") int avgViews,
            @Param("engagementRate") BigDecimal engagementRate,
            @Param("rating") BigDecimal rating,
            @Param("totalReviews") int totalReviews
    );

    @Query("select count(c) from Creator c where c.isVerified = false and c.active = true")
    long countUnverifiedActive();
}
