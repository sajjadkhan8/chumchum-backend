package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.enums.PackageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, UUID> {

    boolean existsByCreatorAndNameIgnoreCase(Creator creator, String name);

    Page<ServicePackage> findByCreator(Creator creator, Pageable pageable);

    List<ServicePackage> findByCreatorIdAndStatus(UUID creatorId, PackageStatus status);

    List<ServicePackage> findByCreatorId(UUID creatorId);

    @Query("""
            select p from ServicePackage p
            where p.status = 'ACTIVE'
              and (:category is null or p.category like %:category%)
              and (:search   is null or p.title   like %:search%)
              and (:minPrice is null or p.price   >= :minPrice)
              and (:maxPrice is null or p.price   <= :maxPrice)
            """)
    Page<ServicePackage> searchActive(
            @Param("category") String category,
            @Param("search")   String search,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            Pageable pageable
    );
}
