package com.chamcham.backend.service;

import com.chamcham.backend.dto.review.ReviewCreateRequest;
import com.chamcham.backend.entity.Gig;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.ReviewMapper;
import com.chamcham.backend.repository.GigRepository;
import com.chamcham.backend.repository.ReviewRepository;
import com.chamcham.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private GigRepository gigRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void createReview_rejectsSeller() {
        assertThatThrownBy(() -> reviewService.createReview(UUID.randomUUID(), true, new ReviewCreateRequest(UUID.randomUUID(), 5, "Great")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Sellers can't create reviews");
    }

    @Test
    void createReview_throwsWhenGigMissing() {
        UUID userId = UUID.randomUUID();
        UUID gigId = UUID.randomUUID();

        when(gigRepository.findById(gigId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(userId, false, new ReviewCreateRequest(gigId, 5, "Great")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Gig not found");
    }

    @Test
    void createReview_throwsWhenUserMissing() {
        UUID userId = UUID.randomUUID();
        UUID gigId = UUID.randomUUID();

        when(gigRepository.findById(gigId)).thenReturn(Optional.of(Gig.builder().id(gigId).build()));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(userId, false, new ReviewCreateRequest(gigId, 5, "Great")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("User not found");
    }
}

