package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.review.ReviewResponse;
import com.chamcham.backend.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getOrder() != null ? review.getOrder().getId() : null,
                review.getCreator() != null ? review.getCreator().getId() : null,
                review.getBrand() != null ? review.getBrand().getId() : null,
                review.getReviewerType() != null ? review.getReviewerType().name().toLowerCase() : "brand",
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
