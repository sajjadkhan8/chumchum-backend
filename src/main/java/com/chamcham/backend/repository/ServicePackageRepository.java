package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.Package;
import com.chamcham.backend.entity.enums.PackageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ServicePackageRepository extends JpaRepository<Package, UUID> {

    boolean existsByCreatorAndNameIgnoreCase(Creator creator, String name);

    Page<Package> findByCreator(Creator creator, Pageable pageable);

    List<Package> findByCreatorIdAndStatus(UUID creatorId, PackageStatus status);

    List<Package> findByCreatorId(UUID creatorId);

    @Query("""
            select p from ServicePackage p
            where p.status = 'ACTIVE'
              and (:category is null or p.category like %:category%)
              and (:search   is null or p.title   like %:search%)
              and (:minPrice is null or p.price   >= :minPrice)
              and (:maxPrice is null or p.price   <= :maxPrice)
            """)
    Page<Package> searchActive(
            @Param("category") String category,
            @Param("search")   String search,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            Pageable pageable
    );

    @Query("""
            select p from ServicePackage p
            where p.status = 'ACTIVE'
              and p.active = true
              and p.visibility = 'public'
              and p.featured = true
            """)
    Page<Package> findFeaturedForFeed(Pageable pageable);
}
