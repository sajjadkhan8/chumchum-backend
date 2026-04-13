package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Gig;
import com.chamcham.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.UUID;

public interface GigRepository extends JpaRepository<Gig, UUID> {

    Page<Gig> findBySeller(User seller, Pageable pageable);

    Page<Gig> findByCategoryContainingIgnoreCaseAndTitleContainingIgnoreCaseAndPriceBetween(
            String category,
            String title,
            BigDecimal min,
            BigDecimal max,
            Pageable pageable
    );
}

