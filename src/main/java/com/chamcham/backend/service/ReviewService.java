package com.chamcham.backend.service;

import com.chamcham.backend.dto.review.ReviewCreateRequest;
import com.chamcham.backend.dto.review.ReviewResponse;
import com.chamcham.backend.entity.Review;
import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.ReviewMapper;
import com.chamcham.backend.repository.ReviewRepository;
import com.chamcham.backend.repository.ServicePackageRepository;
import com.chamcham.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    public ReviewService(
            ReviewRepository reviewRepository,
            ServicePackageRepository servicePackageRepository,
            UserRepository userRepository,
            ReviewMapper reviewMapper
    ) {
        this.reviewRepository = reviewRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
    }

    @Transactional
    public ReviewResponse createReview(UUID userId, UserRole role, ReviewCreateRequest request) {
        if (!role.isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can create reviews!");
        }

        ServicePackage servicePackage = servicePackageRepository.findById(request.packageId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));
        User reviewer = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        reviewRepository.findByServicePackageAndReviewer(servicePackage, reviewer).ifPresent(r -> {
            throw new ApiException(HttpStatus.CONFLICT, "You already reviewed this package");
        });

        Review review = Review.builder()
                .id(UUID.randomUUID())
                .servicePackage(servicePackage)
                .reviewer(reviewer)
                .star(request.star())
                .description(request.description())
                .build();

        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    public List<ReviewResponse> getReviews(UUID packageId) {
        return reviewRepository.findByServicePackageId(packageId).stream().map(reviewMapper::toResponse).toList();
    }
}

