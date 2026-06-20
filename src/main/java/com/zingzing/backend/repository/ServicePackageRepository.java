package com.zingzing.backend.repository;

import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.ServicePackage;
import com.zingzing.backend.entity.enums.DealType;
import com.zingzing.backend.entity.enums.PackageCategory;
import com.zingzing.backend.entity.enums.PackageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, UUID> {

    boolean existsByCreatorAndNameIgnoreCase(Creator creator, String name);

    @EntityGraph(attributePaths = {"creator", "tiers"})
    Page<ServicePackage> findByCreator(Creator creator, Pageable pageable);

    List<ServicePackage> findByCreatorIdAndStatus(UUID creatorId, PackageStatus status);

    List<ServicePackage> findByCreatorId(UUID creatorId);

    @EntityGraph(attributePaths = {"creator", "tiers"})
    @Query("""
            select p from ServicePackage p
            where p.status = 'ACTIVE'
              and (:category is null or p.category = :category)
              and (:search   is null or p.title   like %:search%)
              and (:minPrice is null or p.price   >= :minPrice)
              and (:maxPrice is null or p.price   <= :maxPrice)
            """)
    Page<ServicePackage> searchActive(
            @Param("category") PackageCategory category,
            @Param("search")   String search,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"creator", "tiers"})
    @Query("""
            select p from ServicePackage p
            where p.status = 'ACTIVE'
              and p.active = true
              and p.visibility = 'public'
              and p.featured = true
            """)
    Page<ServicePackage> findFeaturedForFeed(Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "tiers"})
    @Query(value = """
            select p from ServicePackage p
            where p.creator.id = :creatorId
              and (:status is null or p.status = :status)
              and (:dealType is null or p.dealType = :dealType)
              and (:platform is null or lower(cast(p.platform as string)) = lower(cast(:platform as string)))
              and (:search is null
                   or lower(p.title) like concat('%', lower(cast(:search as string)), '%')
                   or lower(p.name) like concat('%', lower(cast(:search as string)), '%'))
            order by p.createdAt desc
            """,
            countQuery = """
            select count(p) from ServicePackage p
            where p.creator.id = :creatorId
              and (:status is null or p.status = :status)
              and (:dealType is null or p.dealType = :dealType)
            """)
    Page<ServicePackage> findByCreatorIdFiltered(
            @Param("creatorId") UUID creatorId,
            @Param("status") PackageStatus status,
            @Param("dealType") DealType dealType,
            @Param("platform") String platform,
            @Param("search") String search,
            Pageable pageable);
}
