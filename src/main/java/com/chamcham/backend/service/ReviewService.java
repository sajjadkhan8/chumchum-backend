package com.chamcham.backend.service;

import com.chamcham.backend.dto.review.ReviewCreateRequest;
import com.chamcham.backend.dto.review.ReviewResponse;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.Review;
import com.chamcham.backend.entity.enums.OrderStatus;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.ReviewMapper;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.OrderRepository;
import com.chamcham.backend.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import static com.chamcham.backend.entity.Review.ReviewerType.BRAND;
import static com.chamcham.backend.entity.Review.ReviewerType.CREATOR;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final CreatorRepository creatorRepository;
    private final BrandRepository brandRepository;
    private final ReviewMapper reviewMapper;

    public ReviewService(ReviewRepository reviewRepository, OrderRepository orderRepository,
                         CreatorRepository creatorRepository, BrandRepository brandRepository,
                         ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.creatorRepository = creatorRepository;
        this.brandRepository = brandRepository;
        this.reviewMapper = reviewMapper;
    }

    @Transactional
    public ReviewResponse createReview(UUID userId, UserRole role, ReviewCreateRequest request) {
        if (!role.isBrand() && !role.isCreator()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands and creators can submit reviews");
        }

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reviews can only be submitted for completed orders");
        }

        Review.ReviewerType reviewerType = role.isBrand() ? BRAND : CREATOR;

        if (role.isBrand() && !order.getBrand().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This order does not belong to you");
        }
        if (role.isCreator() && !order.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This order does not belong to you");
        }

        if (reviewRepository.findByOrderIdAndReviewerType(order.getId(), reviewerType).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "You already reviewed this order");
        }

        Brand brand = order.getBrand();
        Creator creator = order.getCreator();

        Review review = Review.builder()
                .id(UUID.randomUUID())
                .order(order)
                .creator(creator)
                .brand(brand)
                .reviewerType(reviewerType)
                .rating(request.rating())
                .comment(request.comment())
                .servicePackage(order.getServicePackage())
                .reviewer(role.isBrand() ? brand : creator)
                .star(request.rating())
                .description(request.comment() == null || request.comment().isBlank()
                        ? reviewerType.name().toLowerCase() + " review for completed order"
                        : request.comment())
                .build();

        Review saved = reviewRepository.save(review);

        if (role.isBrand()) {
            // Update creator aggregate rating from brand reviews only.
            List<Review> creatorReviews = reviewRepository.findByCreatorIdOrderByCreatedAtDesc(creator.getId())
                    .stream().filter(r -> r.getReviewerType() == BRAND).toList();
            double avg = creatorReviews.stream().mapToInt(Review::getRating).average().orElse(0);
            creator.setRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            creator.setTotalReviews(creatorReviews.size());
            creatorRepository.save(creator);
        } else {
            // Update brand aggregate rating from creator reviews only.
            List<Review> brandReviews = reviewRepository.findByBrandIdOrderByCreatedAtDesc(brand.getId())
                    .stream().filter(r -> r.getReviewerType() == CREATOR).toList();
            double avg = brandReviews.stream().mapToInt(Review::getRating).average().orElse(0);
            brand.setBrandRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            brand.setBrandTotalReviews(brandReviews.size());
            brandRepository.save(brand);
        }

        return reviewMapper.toResponse(saved);
    }

    public List<ReviewResponse> getReviewsByCreator(UUID creatorId) {
        return reviewRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId)
                .stream().filter(r -> r.getReviewerType() == BRAND).map(reviewMapper::toResponse).toList();
    }

    public List<ReviewResponse> getReviewsByBrand(UUID brandId) {
        return reviewRepository.findByBrandIdOrderByCreatedAtDesc(brandId)
                .stream().filter(r -> r.getReviewerType() == CREATOR).map(reviewMapper::toResponse).toList();
    }

    public List<ReviewResponse> getReviewsByPackage(UUID packageId) {
        return reviewRepository.findByServicePackageId(packageId)
                .stream().map(reviewMapper::toResponse).toList();
    }
}
