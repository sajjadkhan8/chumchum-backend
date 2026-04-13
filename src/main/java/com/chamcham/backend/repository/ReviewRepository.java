package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Gig;
import com.chamcham.backend.entity.Review;
import com.chamcham.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByGigId(UUID gigId);

    Optional<Review> findByGigAndReviewer(Gig gig, User reviewer);
}

