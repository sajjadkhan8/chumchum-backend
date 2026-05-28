package com.chamcham.backend.service;

import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.enums.OrderStatus;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.OrderRepository;
import com.chamcham.backend.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final CreatorRepository creatorRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;

    public AnalyticsService(CreatorRepository creatorRepository, OrderRepository orderRepository,
                            ReviewRepository reviewRepository) {
        this.creatorRepository = creatorRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
    }

    public record CreatorDashboard(
            long totalOrders, long activeOrders, long completedOrders,
            long totalEarnings, double avgRating, long totalReviews,
            long repeatBrands
    ) {}

    public record BrandDashboard(
            long totalOrders, long activeOrders, long completedOrders,
            long savedCreators
    ) {}

    public CreatorDashboard creatorDashboard(UUID userId, UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        Creator creator = creatorRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));

        long total = orderRepository.countByCreatorIdAndStatusIn(userId,
                List.of(OrderStatus.values()));
        long active = orderRepository.countByCreatorIdAndStatusIn(userId,
                List.of(OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS, OrderStatus.DELIVERED,
                        OrderStatus.REVIEW, OrderStatus.REVISION));
        long completed = orderRepository.countByCreatorIdAndStatusIn(userId,
                List.of(OrderStatus.COMPLETED));
        long repeatBrands = orderRepository.countDistinctBrandsByCreatorAndCompleted(userId);

        return new CreatorDashboard(total, active, completed, 0L,
                creator.getRating().doubleValue(), creator.getTotalReviews(), repeatBrands);
    }

    public BrandDashboard brandDashboard(UUID userId, UserRole role) {
        if (!role.isBrand()) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        long total = orderRepository.countByBrandIdAndStatusIn(userId, List.of(OrderStatus.values()));
        long active = orderRepository.countByBrandIdAndStatusIn(userId,
                List.of(OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS, OrderStatus.DELIVERED,
                        OrderStatus.REVIEW, OrderStatus.REVISION));
        long completed = orderRepository.countByBrandIdAndStatusIn(userId, List.of(OrderStatus.COMPLETED));

        return new BrandDashboard(total, active, completed, 0L);
    }
}

