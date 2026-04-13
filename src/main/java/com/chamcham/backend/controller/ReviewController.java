package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.review.ReviewCreateRequest;
import com.chamcham.backend.dto.review.ReviewResponse;
import com.chamcham.backend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        ReviewResponse review = reviewService.createReview(authUser.userId(), authUser.seller(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("error", false, "review", review));
    }

    @GetMapping("/{gigId}")
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable UUID gigId) {
        return ResponseEntity.ok(reviewService.getReviews(gigId));
    }
}

