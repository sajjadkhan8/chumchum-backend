package com.zingzing.backend.repository;

import com.zingzing.backend.entity.Review;
import com.zingzing.backend.entity.ServicePackage;
import com.zingzing.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId);

    List<Review> findByBrandIdOrderByCreatedAtDesc(UUID brandId);

    Optional<Review> findByOrderId(UUID orderId);

    Optional<Review> findByOrderIdAndReviewerType(UUID orderId, Review.ReviewerType reviewerType);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.brand.id = :brandId")
    double averageRatingByBrand(@Param("brandId") UUID brandId);

    // legacy support
    List<Review> findByServicePackageId(UUID packageId);

    Optional<Review> findByServicePackageAndReviewer(ServicePackage servicePackage, User reviewer);
}
