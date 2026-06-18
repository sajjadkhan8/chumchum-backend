package com.chamcham.backend.repository;

import com.chamcham.backend.entity.BrandCampaign;
import com.chamcham.backend.entity.enums.BrandCampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BrandCampaignRepository extends JpaRepository<BrandCampaign, UUID> {

    Page<BrandCampaign> findByBrandIdOrderByCreatedAtDesc(UUID brandId, Pageable pageable);

    List<BrandCampaign> findByBrandIdOrderByCreatedAtDesc(UUID brandId);

    @Query("""
            select o from BrandCampaign o
            join fetch o.brand b
            where o.status = 'PUBLISHED'
              and (cast(:search as string) is null
                   or lower(o.title) like concat('%', lower(cast(:search as string)), '%')
                   or lower(o.brief) like concat('%', lower(cast(:search as string)), '%'))
              and (cast(:city as string) is null
                   or lower(coalesce(o.locationTargetingMode, 'nationwide')) in ('nationwide', 'remote_only')
                   or (lower(coalesce(o.locationTargetingMode, '')) = 'cities'
                       and lower(coalesce(o.targetCities, '')) like concat('%', lower(cast(:city as string)), '%'))
                   or lower(coalesce(o.targetCity, '')) = lower(cast(:city as string)))
              and (cast(:type as string) is null or lower(o.offerType) = lower(cast(:type as string)))
              and (cast(:platform as string) is null
                   or lower(coalesce(o.targetPlatforms, '')) like concat('%', lower(cast(:platform as string)), '%'))
              and (cast(:campaignGoal as string) is null
                   or lower(coalesce(o.campaignGoal, '')) like concat('%', lower(cast(:campaignGoal as string)), '%'))
              and (:budgetMin is null or o.budgetMax >= :budgetMin)
              and (:budgetMax is null or o.budgetMin <= :budgetMax)
              and (o.deadlineDate is null or o.deadlineDate >= :today)
            order by o.publishedAt desc nulls last, o.createdAt desc
            """)
    Page<BrandCampaign> findPublishedForCreatorFeed(@Param("search") String search,
                                                 @Param("city") String city,
                                                 @Param("type") String type,
                                                 @Param("platform") String platform,
                                                  @Param("campaignGoal") String campaignGoal,
                                                 @Param("budgetMin") Integer budgetMin,
                                                 @Param("budgetMax") Integer budgetMax,
                                                 @Param("today") LocalDate today,
                                                 Pageable pageable);

    long countByBrandIdAndStatus(UUID brandId, BrandCampaignStatus status);

    long countByBrandIdAndCreatedAtAfter(UUID brandId, Instant since);
}
