package com.zingzing.backend.service;

import com.zingzing.backend.entity.AmbassadorApplication;
import com.zingzing.backend.entity.AmbassadorScore;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.User;
import com.zingzing.backend.entity.enums.AmbassadorAppStatus;
import com.zingzing.backend.entity.enums.AmbassadorTier;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.AmbassadorApplicationRepository;
import com.zingzing.backend.repository.AmbassadorScoreRepository;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.OrderRepository;
import com.zingzing.backend.repository.UserRepository;
import com.zingzing.backend.entity.enums.OrderStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
                // Clear stale review data from prior rejection cycle
                app.setReviewedBy(null);
                app.setNotes(null);
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

        int deliveryScore = Math.min(creator.getCompletedDeals() * 5, 25);
        int ratingScore   = (int)(creator.getRating().doubleValue() * 5);

        // accountAgeScore: 0–15 pts, 3 pts per 6 months of account age, capped at 15
        int accountAgeDays = creator.getCreatedAt() != null
                ? (int) ChronoUnit.DAYS.between(creator.getCreatedAt(), Instant.now())
                : 0;
        int accountAgeScore = Math.min((accountAgeDays / 180) * 3, 15);

        // cancellationScore: 0–10 pts (full 10 when no cancellations; subtract 2 per cancelled order)
        long totalOrders     = orderRepository.countByCreatorIdAndStatusIn(creatorId,
                java.util.List.of(OrderStatus.values()));
        long cancelledOrders = orderRepository.countByCreatorIdAndStatus(creatorId,
                OrderStatus.CANCELLED);
        int cancellationScore = totalOrders == 0 ? 10
                : Math.max(0, 10 - (int) Math.round((cancelledOrders * 10.0 / totalOrders) * 2));

        // profileCompletenessScore: 0–10 pts, 2 pts per completed section (bio, city, avatar, website, social link)
        int completenessScore = 0;
        if (creator.getBio() != null && !creator.getBio().isBlank()) completenessScore += 2;
        if (creator.getCity() != null && !creator.getCity().isBlank()) completenessScore += 2;
        if (creator.getAvatarUrl() != null && !creator.getAvatarUrl().isBlank()) completenessScore += 2;
        if (creator.getWebsite() != null && !creator.getWebsite().isBlank()) completenessScore += 2;
        boolean hasSocialLink = !CollectionUtils.isEmpty(creator.getSocialAccounts());
        if (hasSocialLink) completenessScore += 2;

        // consistencyScore: 0–5 pts based on number of linked social accounts
        int socialCount = creator.getSocialAccounts() == null ? 0 : creator.getSocialAccounts().size();
        int consistencyScore = Math.min(socialCount * 2, 5);

        int total = deliveryScore + ratingScore + accountAgeScore + cancellationScore
                + completenessScore + consistencyScore;
        total = Math.min(total + (creator.getTotalReviews() > 10 ? 20 : creator.getTotalReviews() * 2)
                + (creator.getFollowers() > 10000 ? 15 : creator.getFollowers() / 1000), MAX_SCORE);

        AmbassadorTier tier = tierFromScore(total);

        AmbassadorScore score = scoreRepo.findByCreatorId(creatorId)
                .orElse(AmbassadorScore.builder().creator(creator).build());
        score.setTotal(total);
        score.setTier(tier);
        score.setDeliveryScore(deliveryScore);
        score.setRatingScore(ratingScore);
        score.setAccountAgeScore(accountAgeScore);
        score.setCancellationScore(cancellationScore);
        score.setProfileCompletenessScore(completenessScore);
        score.setConsistencyScore(consistencyScore);
        return scoreRepo.save(score);
    }

    public List<Creator> getAmbassadors(int limit) {
        int safeLimit = Math.min(limit, 100);
        Page<AmbassadorApplication> approved = applicationRepo.findByStatus(
                AmbassadorAppStatus.APPROVED, PageRequest.of(0, safeLimit));
        return approved.getContent().stream()
                .map(AmbassadorApplication::getCreator)
                .toList();
    }

    // ---- Admin ----
    public Page<AmbassadorApplication> listApplications(String search, String status, Pageable pageable) {
        AmbassadorAppStatus statusFilter = null;
        try {
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
                statusFilter = AmbassadorAppStatus.valueOf(status.trim().toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }
        String searchFilter = search == null || search.isBlank() ? null : search.trim();
        return applicationRepo.searchForAdmin(searchFilter, statusFilter, pageable);
    }

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
