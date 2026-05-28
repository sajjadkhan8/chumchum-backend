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

import java.util.List;
import java.util.UUID;

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
    public ReviewResponse createReview(UUID brandUserId, UserRole role, ReviewCreateRequest request) {
        if (!role.isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can submit reviews");
        }
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getBrand().getId().equals(brandUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This order does not belong to you");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reviews can only be submitted for completed orders");
        }
        if (reviewRepository.findByOrderId(order.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "You already reviewed this order");
        }

        Brand brand = brandRepository.findById(brandUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand not found"));
        Creator creator = order.getCreator();

        Review review = Review.builder()
                .id(UUID.randomUUID())
                .order(order)
                .creator(creator)
                .brand(brand)
                .rating(request.rating())
                .comment(request.comment())
                .build();

        Review saved = reviewRepository.save(review);

        // update creator aggregate rating
        List<Review> allReviews = reviewRepository.findByCreatorIdOrderByCreatedAtDesc(creator.getId());
        double avgRating = allReviews.stream().mapToInt(Review::getRating).average().orElse(0);
        creator.setRating(new java.math.BigDecimal(String.format("%.2f", avgRating)));
        creator.setTotalReviews(allReviews.size());
        creatorRepository.save(creator);

        return reviewMapper.toResponse(saved);
    }

    public List<ReviewResponse> getReviewsByCreator(UUID creatorId) {
        return reviewRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId)
                .stream().map(reviewMapper::toResponse).toList();
    }

    public List<ReviewResponse> getReviewsByPackage(UUID packageId) {
        return reviewRepository.findByServicePackageId(packageId)
                .stream().map(reviewMapper::toResponse).toList();
    }
}
