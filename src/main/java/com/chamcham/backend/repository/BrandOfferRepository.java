package com.chamcham.backend.repository;

import com.chamcham.backend.entity.BrandOffer;
import com.chamcham.backend.entity.enums.BrandOfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BrandOfferRepository extends JpaRepository<BrandOffer, UUID> {

    Page<BrandOffer> findByBrandIdOrderByCreatedAtDesc(UUID brandId, Pageable pageable);

    List<BrandOffer> findByBrandIdOrderByCreatedAtDesc(UUID brandId);

    @Query("""
            select o from BrandOffer o
            join fetch o.brand b
            where o.status = 'PUBLISHED'
              and (cast(:search as string) is null
                   or lower(o.title) like concat('%', lower(cast(:search as string)), '%')
                   or lower(o.brief) like concat('%', lower(cast(:search as string)), '%'))
              and (cast(:city as string) is null or lower(o.targetCity) = lower(cast(:city as string)))
              and (cast(:type as string) is null or lower(o.offerType) = lower(cast(:type as string)))
              and (:budgetMin is null or o.budgetMax >= :budgetMin)
              and (:budgetMax is null or o.budgetMin <= :budgetMax)
              and (:minFollowers is null or o.minFollowers is null or o.minFollowers <= :minFollowers)
              and (o.deadlineDate is null or o.deadlineDate >= :today)
            order by o.publishedAt desc nulls last, o.createdAt desc
            """)
    Page<BrandOffer> findPublishedForCreatorFeed(@Param("search") String search,
                                                 @Param("city") String city,
                                                 @Param("type") String type,
                                                 @Param("budgetMin") Integer budgetMin,
                                                 @Param("budgetMax") Integer budgetMax,
                                                 @Param("minFollowers") Integer minFollowers,
                                                 @Param("today") LocalDate today,
                                                 Pageable pageable);

    long countByBrandIdAndStatus(UUID brandId, BrandOfferStatus status);
}

