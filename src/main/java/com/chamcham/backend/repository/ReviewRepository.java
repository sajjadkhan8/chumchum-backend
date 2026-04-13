package com.chamcham.backend.repository;

import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.Review;
import com.chamcham.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByServicePackageId(UUID packageId);

    Optional<Review> findByServicePackageAndReviewer(ServicePackage servicePackage, User reviewer);
}

