package com.chamcham.backend.repository;

import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.Creator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.UUID;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, UUID> {

    boolean existsByCreatorAndNameIgnoreCase(Creator creator, String name);

    Page<ServicePackage> findByCreator(Creator creator, Pageable pageable);

    Page<ServicePackage> findByActiveTrueAndCategoryContainingIgnoreCaseAndTitleContainingIgnoreCaseAndPriceBetween(
            String category,
            String title,
            BigDecimal min,
            BigDecimal max,
            Pageable pageable
    );
}



