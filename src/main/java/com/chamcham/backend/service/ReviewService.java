package com.chamcham.backend.service;

import com.chamcham.backend.dto.review.ReviewCreateRequest;
import com.chamcham.backend.dto.review.ReviewResponse;
import com.chamcham.backend.entity.Gig;
import com.chamcham.backend.entity.Review;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.ReviewMapper;
import com.chamcham.backend.repository.GigRepository;
import com.chamcham.backend.repository.ReviewRepository;
import com.chamcham.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final GigRepository gigRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    public ReviewService(ReviewRepository reviewRepository, GigRepository gigRepository, UserRepository userRepository, ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.gigRepository = gigRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
    }

    @Transactional
    public ReviewResponse createReview(UUID userId, UserRole role, ReviewCreateRequest request) {
        if (!role.isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can create reviews!");
        }

        Gig gig = gigRepository.findById(request.gigId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Gig not found"));
        User reviewer = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        reviewRepository.findByGigAndReviewer(gig, reviewer).ifPresent(r -> {
            throw new ApiException(HttpStatus.CONFLICT, "You already reviewed this gig");
        });

        Review review = Review.builder()
                .id(UUID.randomUUID())
                .gig(gig)
                .reviewer(reviewer)
                .star(request.star())
                .description(request.description())
                .build();

        gig.setTotalStars(gig.getTotalStars() + request.star());
        gig.setStarNumber(gig.getStarNumber() + 1);

        gigRepository.save(gig);
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    public List<ReviewResponse> getReviews(UUID gigId) {
        return reviewRepository.findByGigId(gigId).stream().map(reviewMapper::toResponse).toList();
    }
}

