package com.chamcham.backend.service;

import com.chamcham.backend.entity.AmbassadorApplication;
import com.chamcham.backend.entity.AmbassadorScore;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.AmbassadorAppStatus;
import com.chamcham.backend.entity.enums.AmbassadorTier;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.AmbassadorApplicationRepository;
import com.chamcham.backend.repository.AmbassadorScoreRepository;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.OrderRepository;
import com.chamcham.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AmbassadorService {

    private static final int MAX_SCORE = 100;

    private final AmbassadorApplicationRepository applicationRepo;
    private final AmbassadorScoreRepository scoreRepo;
    private final CreatorRepository creatorRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AmbassadorService(AmbassadorApplicationRepository applicationRepo,
                             AmbassadorScoreRepository scoreRepo,
                             CreatorRepository creatorRepository,
                             OrderRepository orderRepository,
                             UserRepository userRepository) {
        this.applicationRepo = applicationRepo;
        this.scoreRepo = scoreRepo;
        this.creatorRepository = creatorRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    public AmbassadorApplication getMyApplication(UUID userId, UserRole role) {
        requireCreator(role);
        return applicationRepo.findByCreatorId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No application found"));
    }

    @Transactional
    public AmbassadorApplication applyOrResubmit(UUID userId, UserRole role) {
        requireCreator(role);
        Creator creator = findCreator(userId);

        return applicationRepo.findByCreatorId(userId).map(app -> {
            if (app.getStatus() == AmbassadorAppStatus.REJECTED || app.getStatus() == AmbassadorAppStatus.DRAFT) {
                app.setStatus(AmbassadorAppStatus.SUBMITTED);
                app.setSubmittedAt(Instant.now());
                return applicationRepo.save(app);
            }
            throw new ApiException(HttpStatus.CONFLICT, "Application already submitted or under review");
        }).orElseGet(() -> {
            AmbassadorApplication app = AmbassadorApplication.builder()
                    .creator(creator)
                    .status(AmbassadorAppStatus.SUBMITTED)
                    .submittedAt(Instant.now())
                    .build();
            return applicationRepo.save(app);
        });
    }

    public AmbassadorScore getScore(UUID userId, UserRole role) {
        requireCreator(role);
        return scoreRepo.findByCreatorId(userId)
                .orElseGet(() -> computeAndSave(userId));
    }

    @Transactional
    public AmbassadorScore computeAndSave(UUID creatorId) {
        Creator creator = findCreator(creatorId);
        int total = computeScore(creator);
        AmbassadorTier tier = tierFromScore(total);

        AmbassadorScore score = scoreRepo.findByCreatorId(creatorId)
                .orElse(AmbassadorScore.builder().creator(creator).build());
        score.setTotal(total);
        score.setTier(tier);
        score.setDeliveryScore(Math.min(creator.getCompletedDeals() * 5, 25));
        score.setRatingScore((int)(creator.getRating().doubleValue() * 5));
        return scoreRepo.save(score);
    }

    public List<Creator> getAmbassadors(int limit) {
        return creatorRepository.findByIsVerifiedTrue(PageRequest.of(0, limit)).getContent();
    }

    // ---- Admin ----
    public AmbassadorApplication getApplicationById(UUID id) {
        return applicationRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    @Transactional
    public AmbassadorApplication reviewApplication(UUID id, AmbassadorAppStatus newStatus,
                                                    UUID reviewerUserId, String notes) {
        AmbassadorApplication app = getApplicationById(id);
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reviewer not found"));
        app.setStatus(newStatus);
        app.setReviewedBy(reviewer);
        app.setNotes(notes);
        if (newStatus == AmbassadorAppStatus.APPROVED) {
            app.setApprovedAt(Instant.now());
            // mark creator as verified ambassador
            Creator creator = app.getCreator();
            creator.setVerified(true);
            creatorRepository.save(creator);
        }
        return applicationRepo.save(app);
    }

    // ---- helpers ----
    private int computeScore(Creator c) {
        int score = 0;
        score += Math.min(c.getCompletedDeals() * 5, 25); // up to 25
        score += (int)(c.getRating().doubleValue() * 5);  // up to 25
        score += c.getTotalReviews() > 10 ? 20 : c.getTotalReviews() * 2; // up to 20
        score += c.getFollowers() > 10000 ? 15 : c.getFollowers() / 1000; // up to 15
        if (c.getBio() != null && c.getCoverImageUrl() != null) score += 10;
        if (c.getTiktokUrl() != null || c.getInstagramUrl() != null) score += 5;
        return Math.min(score, MAX_SCORE);
    }

    private AmbassadorTier tierFromScore(int score) {
        if (score >= 80) return AmbassadorTier.ELITE_AMBASSADOR;
        if (score >= 60) return AmbassadorTier.VERIFIED_AMBASSADOR;
        if (score >= 40) return AmbassadorTier.EMERGING_AMBASSADOR;
        return AmbassadorTier.RISING_CREATOR;
    }

    private void requireCreator(UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can access ambassador features");
    }

    private Creator findCreator(UUID id) {
        return creatorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
    }
}

