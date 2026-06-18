package com.zingzing.backend.repository;

import com.zingzing.backend.entity.BrandCampaignReaction;
import com.zingzing.backend.entity.enums.BrandCampaignReactionStatus;
import com.zingzing.backend.entity.enums.BrandCampaignReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandCampaignReactionRepository extends JpaRepository<BrandCampaignReaction, UUID> {

    @Query("""
            select r from BrandCampaignReaction r
            join fetch r.creator c
            where r.campaign.id = :campaignId
              and (:status is null or r.status = :status)
              and (:reactionType is null or r.reactionType = :reactionType)
            order by r.createdAt desc
            """)
    Page<BrandCampaignReaction> findByCampaignIdWithFilters(@Param("campaignId") UUID campaignId,
                                                      @Param("status") BrandCampaignReactionStatus status,
                                                      @Param("reactionType") BrandCampaignReactionType reactionType,
                                                      Pageable pageable);

    // Non-paginated used for response mapping during single campaign load
    @Query("""
            select r from BrandCampaignReaction r
            join fetch r.creator c
            where r.campaign.id = :campaignId
            order by r.createdAt desc
            """)
    List<BrandCampaignReaction> findByCampaignIdWithCreator(@Param("campaignId") UUID campaignId);

    @Query("""
            select r from BrandCampaignReaction r
            join fetch r.campaign o
            join fetch o.brand b
            where r.creator.id = :creatorId
            order by r.updatedAt desc
            """)
    Page<BrandCampaignReaction> findByCreatorIdWithCampaign(@Param("creatorId") UUID creatorId, Pageable pageable);

    Optional<BrandCampaignReaction> findByCampaignIdAndCreatorId(UUID campaignId, UUID creatorId);

    long countByCampaignId(UUID campaignId);
}
