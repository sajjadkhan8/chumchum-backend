package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Review;
import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId);

    Optional<Review> findByOrderId(UUID orderId);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.brand.id = :brandId")
    double averageRatingByBrand(@Param("brandId") UUID brandId);

    // legacy support
    List<Review> findByServicePackageId(UUID packageId);

    Optional<Review> findByServicePackageAndReviewer(ServicePackage servicePackage, User reviewer);
}
