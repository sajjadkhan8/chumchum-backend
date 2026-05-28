package com.chamcham.backend.service;

import com.chamcham.backend.dto.creator.CreatorCreateRequest;
import com.chamcham.backend.dto.creator.CreatorResponse;
import com.chamcham.backend.dto.creator.CreatorUpdateRequest;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.PayoutMethod;
import com.chamcham.backend.entity.SocialAccount;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.PayoutMethodType;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.CreatorMapper;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.PayoutMethodRepository;
import com.chamcham.backend.repository.SocialAccountRepository;
import com.chamcham.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CreatorService {

    private final CreatorRepository creatorRepository;
    private final UserRepository userRepository;
    private final CreatorMapper creatorMapper;
    private final SocialAccountRepository socialAccountRepository;
    private final PayoutMethodRepository payoutMethodRepository;

    public CreatorService(CreatorRepository creatorRepository, UserRepository userRepository,
                          CreatorMapper creatorMapper,
                          SocialAccountRepository socialAccountRepository,
                          PayoutMethodRepository payoutMethodRepository) {
        this.creatorRepository = creatorRepository;
        this.userRepository = userRepository;
        this.creatorMapper = creatorMapper;
        this.socialAccountRepository = socialAccountRepository;
        this.payoutMethodRepository = payoutMethodRepository;
    }

    @Transactional
    public CreatorResponse create(CreatorCreateRequest request) {
        if (request.userId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getRole().isCreator()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User role must be CREATOR");
        }

        if (creatorRepository.findById(user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Creator profile already exists for this user");
        }

        int insertedRows = creatorRepository.insertProfile(
                user.getId(),
                request.bio(),
                request.category(),
                request.tiktokUrl(),
                request.instagramUrl(),
                request.youtubeUrl(),
                request.facebookUrl(),
                0,
                0,
                null,
                BigDecimal.ZERO,
                0
        );

        if (insertedRows != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create creator profile");
        }

        Creator created = creatorRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Creator profile created but could not be loaded"));
        return creatorMapper.toResponse(created);
    }

    @Transactional
    public List<CreatorResponse> getAll() {
        return creatorRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(creatorMapper::toResponse)
                .toList();
    }

    public record CreatorSearchResult(
            List<CreatorResponse> creators, long total, int page, int limit) {}

    public CreatorSearchResult search(
            String search, String city,
            Integer minFollowers, Integer maxFollowers,
            BigDecimal minRating, Integer minPrice, Integer maxPrice,
            Boolean acceptsBarter, Boolean isTrending, Boolean isFastResponder,
            Boolean ambassadorOnly,
            int page, int limit, String sortBy) {

        String sortField = switch (sortBy == null ? "" : sortBy) {
            case "top_rated"       -> "rating";
            case "budget_friendly" -> "minPrice";
            default                -> "createdAt";
        };

        Boolean isVerified = Boolean.TRUE.equals(ambassadorOnly) ? Boolean.TRUE : null;

        Pageable pageable = PageRequest.of(page, Math.min(limit, 50),
                Sort.by(Sort.Direction.DESC, sortField));

        Page<Creator> result = creatorRepository.search(
                search, city, minFollowers, maxFollowers, minRating,
                minPrice, maxPrice, acceptsBarter, isTrending, isFastResponder,
                isVerified, pageable);

        return new CreatorSearchResult(
                result.getContent().stream().map(creatorMapper::toResponse).toList(),
                result.getTotalElements(), page, limit);
    }

    @Transactional
    public CreatorResponse getById(UUID creatorId) {
        return creatorMapper.toResponse(findCreator(creatorId));
    }

    @Transactional
    public CreatorResponse getByUserId(UUID actorUserId, UserRole actorRole, UUID userId) {
        validateOwnerOrAdmin(actorUserId, actorRole, userId);
        Creator creator = creatorRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
        return creatorMapper.toResponse(creator);
    }

    @Transactional
    public CreatorResponse update(UUID actorUserId, UserRole actorRole, UUID creatorId, CreatorUpdateRequest request) {
        Creator creator = findCreator(creatorId);
        validateOwnerOrAdmin(actorUserId, actorRole, creator.getId());
        validateMetricsAccess(actorRole, request);

        if (request.bio() != null) {
            creator.setBio(request.bio());
        }
        if (request.category() != null) {
            creator.setCategory(request.category());
        }
        if (request.tiktokUrl() != null) {
            creator.setTiktokUrl(request.tiktokUrl());
        }
        if (request.instagramUrl() != null) {
            creator.setInstagramUrl(request.instagramUrl());
        }
        if (request.youtubeUrl() != null) {
            creator.setYoutubeUrl(request.youtubeUrl());
        }
        if (request.facebookUrl() != null) {
            creator.setFacebookUrl(request.facebookUrl());
        }
        if (request.followers() != null) {
            creator.setFollowers(request.followers());
        }
        if (request.avgViews() != null) {
            creator.setAvgViews(request.avgViews());
        }
        if (request.engagementRate() != null) {
            creator.setEngagementRate(request.engagementRate());
        }
        if (request.rating() != null) {
            creator.setRating(request.rating());
        }
        if (request.totalReviews() != null) {
            creator.setTotalReviews(request.totalReviews());
        }

        return creatorMapper.toResponse(creatorRepository.save(creator));
    }

    // ---- Social accounts ----

    public record SocialAccountRequest(
            String platform, String username, String profileUrl,
            Integer followers, Integer avgViews, BigDecimal engagementRate) {}

    @Transactional
    public List<SocialAccount> updateSocialAccounts(UUID userId, UserRole role, List<SocialAccountRequest> accounts) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can update social accounts");
        Creator creator = findCreator(userId);
        // delete existing and re-insert (simpler than upsert per-platform)
        socialAccountRepository.deleteAll(socialAccountRepository.findByCreatorId(userId));
        List<SocialAccount> saved = accounts.stream().map(a -> socialAccountRepository.save(
                SocialAccount.builder()
                        .creator(creator)
                        .platform(a.platform())
                        .username(a.username() != null ? a.username() : "")
                        .profileUrl(a.profileUrl())
                        .followers(a.followers() != null ? a.followers() : 0)
                        .avgViews(a.avgViews())
                        .engagementRate(a.engagementRate() != null ? a.engagementRate() : BigDecimal.ZERO)
                        .build()
        )).toList();
        return saved;
    }

    // ---- Preferences ----

    public record PreferencesRequest(
            Boolean acceptsBarter, Boolean acceptsHybridDeals,
            String preferredIndustries, Integer minimumBudget) {}

    @Transactional
    public CreatorResponse updatePreferences(UUID userId, UserRole role, PreferencesRequest req) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can update preferences");
        Creator creator = findCreator(userId);
        if (req.acceptsBarter() != null) creator.setAcceptsBarter(req.acceptsBarter());
        if (req.acceptsHybridDeals() != null) creator.setAcceptsHybridDeals(req.acceptsHybridDeals());
        if (req.preferredIndustries() != null) creator.setPreferredIndustries(req.preferredIndustries());
        if (req.minimumBudget() != null) creator.setMinimumBudget(req.minimumBudget());
        return creatorMapper.toResponse(creatorRepository.save(creator));
    }

    // ---- Payment settings ----

    public record PaymentSettingsRequest(
            String stcPayNumber, String madaCard, String accountTitle,
            String ibanOrAccount, String applePayNumber, String bankTransferIban) {}

    @Transactional
    public void updatePaymentSettings(UUID userId, UserRole role, PaymentSettingsRequest req) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can update payment settings");
        Creator creator = findCreator(userId);
        upsertPayoutMethod(creator, PayoutMethodType.STCPAY, "STC Pay", req.stcPayNumber());
        upsertPayoutMethod(creator, PayoutMethodType.MADA,    "Mada",    req.madaCard());
        upsertPayoutMethod(creator, PayoutMethodType.APPLEPAY,"Apple Pay", req.applePayNumber());
        upsertPayoutMethod(creator, PayoutMethodType.BANK_TRANSFER, req.accountTitle() != null ? req.accountTitle() : "Bank Transfer",
                req.ibanOrAccount() != null ? req.ibanOrAccount() : req.bankTransferIban());
    }

    private void upsertPayoutMethod(Creator creator, PayoutMethodType type, String name, String accountDetails) {
        if (accountDetails == null || accountDetails.isBlank()) return;
        List<PayoutMethod> existing = payoutMethodRepository.findByCreatorId(creator.getId())
                .stream().filter(p -> p.getType() == type).toList();
        if (!existing.isEmpty()) {
            PayoutMethod pm = existing.get(0);
            pm.setAccountDetails(accountDetails);
            pm.setName(name);
            payoutMethodRepository.save(pm);
        } else {
            payoutMethodRepository.save(PayoutMethod.builder()
                    .creator(creator).type(type).name(name).accountDetails(accountDetails).build());
        }
    }

    private Creator findCreator(UUID creatorId) {
        return creatorRepository.findById(creatorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
    }

    private void validateOwnerOrAdmin(UUID actorUserId, UserRole actorRole, UUID resourceUserId) {
        if (!actorRole.isAdmin() && !actorUserId.equals(resourceUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage your own creator profile");
        }
    }

    private void validateMetricsAccess(UserRole actorRole, CreatorUpdateRequest request) {
        boolean metricsProvided = request.followers() != null
                || request.avgViews() != null
                || request.engagementRate() != null
                || request.rating() != null
                || request.totalReviews() != null;

        if (metricsProvided && !actorRole.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only admin can update creator metrics");
        }
    }
}
