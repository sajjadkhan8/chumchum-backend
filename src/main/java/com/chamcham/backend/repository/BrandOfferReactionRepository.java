package com.chamcham.backend.repository;

import com.chamcham.backend.entity.BrandOfferReaction;
import com.chamcham.backend.entity.enums.BrandOfferReactionStatus;
import com.chamcham.backend.entity.enums.BrandOfferReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandOfferReactionRepository extends JpaRepository<BrandOfferReaction, UUID> {

    @Query("""
            select r from BrandOfferReaction r
            join fetch r.creator c
            where r.offer.id = :offerId
              and (:status is null or r.status = :status)
              and (:reactionType is null or r.reactionType = :reactionType)
            order by r.createdAt desc
            """)
    Page<BrandOfferReaction> findByOfferIdWithFilters(@Param("offerId") UUID offerId,
                                                      @Param("status") BrandOfferReactionStatus status,
                                                      @Param("reactionType") BrandOfferReactionType reactionType,
                                                      Pageable pageable);

    // Non-paginated used for response mapping during single offer load
    @Query("""
            select r from BrandOfferReaction r
            join fetch r.creator c
            where r.offer.id = :offerId
            order by r.createdAt desc
            """)
    List<BrandOfferReaction> findByOfferIdWithCreator(@Param("offerId") UUID offerId);

    @Query("""
            select r from BrandOfferReaction r
            join fetch r.offer o
            join fetch o.brand b
            where r.creator.id = :creatorId
            order by r.updatedAt desc
            """)
    Page<BrandOfferReaction> findByCreatorIdWithOffer(@Param("creatorId") UUID creatorId, Pageable pageable);

    Optional<BrandOfferReaction> findByOfferIdAndCreatorId(UUID offerId, UUID creatorId);

    long countByOfferId(UUID offerId);
}

