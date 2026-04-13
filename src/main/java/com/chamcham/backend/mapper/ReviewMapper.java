package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.review.ReviewResponse;
import com.chamcham.backend.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    private final UserMapper userMapper;

    public ReviewMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getServicePackage().getId(),
                userMapper.toResponse(review.getReviewer()),
                review.getStar(),
                review.getDescription(),
                review.getCreatedAt()
        );
    }
}

