package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.entity.AmbassadorApplication;
import com.zingzing.backend.entity.AmbassadorScore;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.enums.AmbassadorAppStatus;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.mapper.CreatorMapper;
import com.zingzing.backend.service.AmbassadorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ambassador")
public class AmbassadorController {

    private final AmbassadorService ambassadorService;
    private final CreatorMapper creatorMapper;

    public AmbassadorController(AmbassadorService ambassadorService, CreatorMapper creatorMapper) {
        this.ambassadorService = ambassadorService;
        this.creatorMapper = creatorMapper;
    }

    public record ReviewRequest(
            @Size(max = 30) String status,
            @Size(max = 2000) String notes
    ) {}

    // ---- Creator endpoints ----

    @GetMapping("/application")
    public ResponseEntity<Map<String, Object>> getMyApplication(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        AmbassadorApplication app = ambassadorService.getMyApplication(authUser.userId(), authUser.role());
        return ResponseEntity.ok(Map.of("success", true, "data", toApplicationMap(app)));
    }

    @PostMapping("/application")
    public ResponseEntity<Map<String, Object>> apply(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        AmbassadorApplication app = ambassadorService.applyOrResubmit(authUser.userId(), authUser.role());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "data", toApplicationMap(app)));
    }

    @GetMapping("/score")
    public ResponseEntity<Map<String, Object>> getScore(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        AmbassadorScore score = ambassadorService.getScore(authUser.userId(), authUser.role());
        return ResponseEntity.ok(Map.of("success", true, "data", toScoreMap(score)));
    }

    // ---- Public ----

    @GetMapping("/ambassadors")
    public ResponseEntity<Map<String, Object>> listAmbassadors(
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<Creator> ambassadors = ambassadorService.getAmbassadors(Math.min(limit, 100));
        return ResponseEntity.ok(Map.of("success", true, "data",
                ambassadors.stream().map(creatorMapper::toResponse).toList()));
    }

    @GetMapping("/benefits")
    public ResponseEntity<Map<String, Object>> benefits() {
        List<Map<String, String>> benefits = List.of(
                Map.of(
                        "title", "Monthly Guaranteed Income",
                        "description", "Starting from PKR 1,250,000/month for eligible ambassadors.",
                        "icon", "💰"
                ),
                Map.of(
                        "title", "Direct Brand Access",
                        "description", "Priority access to premium and enterprise campaigns.",
                        "icon", "🤝"
                ),
                Map.of(
                        "title", "Dedicated Account Manager",
                        "description", "Personalized support for campaign planning and growth.",
                        "icon", "💼"
                ),
                Map.of(
                        "title", "Performance Bonuses",
                        "description", "Tier-based bonus incentives based on delivery quality.",
                        "icon", "🏆"
                )
        );
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("benefits", benefits)));
    }

    @GetMapping({"/eligibility", "/requirements"})
    public ResponseEntity<Map<String, Object>> eligibilityRequirements() {
        Map<String, Object> requirements = Map.of(
                "minFollowers", 100_000,
                "minEngagementRate", 5,
                "minRating", 4.5,
                "minCompletedDeals", 30,
                "verificationSteps", List.of(
                        "Identity Verification (CNIC)",
                        "Tax Profile Verification (NTN/STRN where applicable)",
                        "Engagement Metrics Verification",
                        "Content Quality & Brand Safety Review",
                        "Background & Compliance Check"
                )
        );
        return ResponseEntity.ok(Map.of("success", true, "data", requirements));
    }

    // ---- Admin endpoints ----

    @GetMapping("/applications")
    public ResponseEntity<Map<String, Object>> listApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        requireAdmin(authUser);
        Page<AmbassadorApplication> result = ambassadorService.listApplications(
                null, status, PageRequest.of(page, size, Sort.by("submittedAt").descending()));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result.getContent().stream().map(this::toApplicationMap).toList(),
                "total", result.getTotalElements(),
                "page", result.getNumber(),
                "size", result.getSize()
        ));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<Map<String, Object>> getApplicationById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        requireAdmin(authUser);
        AmbassadorApplication app = ambassadorService.getApplicationById(id);
        return ResponseEntity.ok(Map.of("success", true, "data", toApplicationMap(app)));
    }

    @PatchMapping("/applications/{id}")
    public ResponseEntity<Map<String, Object>> reviewApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody ReviewRequest req
    ) {
        requireAdmin(authUser);
        AmbassadorAppStatus newStatus;
        try {
            newStatus = AmbassadorAppStatus.valueOf(req.status().trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status value: " + req.status());
        }
        AmbassadorApplication app = ambassadorService.reviewApplication(id, newStatus, authUser.userId(), req.notes());
        return ResponseEntity.ok(Map.of("success", true, "data", toApplicationMap(app)));
    }

    // ---- Helpers ----

    private void requireAdmin(AuthenticatedUser authUser) {
        if (!authUser.role().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private Map<String, Object> toApplicationMap(AmbassadorApplication app) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", app.getId());
        m.put("creatorId", app.getCreator().getId());
        m.put("status", app.getStatus().name().toLowerCase());
        m.put("submittedAt", app.getSubmittedAt());
        Map<String, Object> verificationSteps = new LinkedHashMap<>();
        verificationSteps.put("identityVerified", app.isIdentityVerified());
        verificationSteps.put("engagementVerified", app.isEngagementVerified());
        verificationSteps.put("contentReviewPassed", app.isContentReviewPassed());
        verificationSteps.put("backgroundCheckPassed", app.isBackgroundCheckPassed());
        m.put("verificationSteps", verificationSteps);
        m.put("notes", app.getNotes());
        m.put("rejectionReason", app.getRejectionReason());
        m.put("approvedAt", app.getApprovedAt());
        m.put("createdAt", app.getCreatedAt());
        return m;
    }

    private Map<String, Object> toScoreMap(AmbassadorScore score) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", score.getTotal());
        m.put("tier", score.getTier().name().toLowerCase());
        m.put("percentileRank", score.getPercentileRank());
        m.put("deliveryScore", score.getDeliveryScore());
        m.put("ratingScore", score.getRatingScore());
        m.put("accountAgeScore", score.getAccountAgeScore());
        m.put("cancellationScore", score.getCancellationScore());
        m.put("profileCompletenessScore", score.getProfileCompletenessScore());
        m.put("consistencyScore", score.getConsistencyScore());
        m.put("strengths", score.getStrengths());
        m.put("improvements", score.getImprovements());
        m.put("calculatedAt", score.getCalculatedAt());
        return m;
    }
}
