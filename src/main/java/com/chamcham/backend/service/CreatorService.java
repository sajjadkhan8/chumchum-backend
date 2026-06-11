package com.chamcham.backend.service;

import com.chamcham.backend.dto.creator.CreatorCreateRequest;
import com.chamcham.backend.dto.creator.CreatorResponse;
import com.chamcham.backend.dto.creator.CreatorUpdateRequest;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.CreatorPayoutPreference;
import com.chamcham.backend.entity.PayoutMethod;
import com.chamcham.backend.entity.SocialAccount;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.CreatorBadgeLevel;
import com.chamcham.backend.entity.enums.CreatorPayoutSchedule;
import com.chamcham.backend.entity.enums.PayoutMethodType;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.CreatorMapper;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.CreatorPayoutPreferenceRepository;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CreatorService {

    private final CreatorRepository creatorRepository;
    private final UserRepository userRepository;
    private final CreatorMapper creatorMapper;
    private final SocialAccountRepository socialAccountRepository;
    private final PayoutMethodRepository payoutMethodRepository;
    private final CreatorPayoutPreferenceRepository creatorPayoutPreferenceRepository;
    private final PaymentValidationService paymentValidationService;
    private final PaymentAuditService paymentAuditService;

    public CreatorService(CreatorRepository creatorRepository, UserRepository userRepository,
                          CreatorMapper creatorMapper,
                          SocialAccountRepository socialAccountRepository,
                          PayoutMethodRepository payoutMethodRepository,
                          CreatorPayoutPreferenceRepository creatorPayoutPreferenceRepository,
                          PaymentValidationService paymentValidationService,
                          PaymentAuditService paymentAuditService) {
        this.creatorRepository = creatorRepository;
        this.userRepository = userRepository;
        this.creatorMapper = creatorMapper;
        this.socialAccountRepository = socialAccountRepository;
        this.payoutMethodRepository = payoutMethodRepository;
        this.creatorPayoutPreferenceRepository = creatorPayoutPreferenceRepository;
        this.paymentValidationService = paymentValidationService;
        this.paymentAuditService = paymentAuditService;
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
                .map(creatorMapper::toPublicResponse)
                .toList();
    }

    public record CreatorSearchResult(
            List<CreatorResponse> creators, long total, int page, int limit) {}

    public CreatorSearchResult search(
            String search, String city,
            Integer minFollowers, Integer maxFollowers,
            BigDecimal minRating, Integer minPrice, Integer maxPrice,
            CreatorBadgeLevel badgeLevel, String availabilityStatus,
            Boolean acceptsBarter, Boolean isTrending, Boolean isFastResponder,
            Boolean ambassadorOnly,
            int page, int limit, String sortBy) {

        String normalizedSort = sortBy == null ? "" : sortBy;
        String sortField = switch (normalizedSort) {
            case "trending"        -> "isTrending";
            case "top_rated"       -> "rating";
            case "budget_friendly" -> "minPrice";
            default                -> "createdAt";
        };
        Sort.Direction sortDirection = "budget_friendly".equals(normalizedSort)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Boolean isVerified = Boolean.TRUE.equals(ambassadorOnly) ? Boolean.TRUE : null;

        Pageable pageable = cappedPageable(page, limit, sortField, sortDirection);

        Page<Creator> result = creatorRepository.search(
                search, city, minFollowers, maxFollowers, minRating,
                minPrice, maxPrice, badgeLevel,
                availabilityStatus == null || availabilityStatus.isBlank() ? null : availabilityStatus.trim(),
                acceptsBarter, isTrending, isFastResponder,
                isVerified, pageable);

        return new CreatorSearchResult(
                result.getContent().stream().map(creatorMapper::toPublicResponse).toList(),
                result.getTotalElements(), page, limit);
    }

    @Transactional
    public CreatorResponse getById(UUID creatorId) {
        return creatorMapper.toPublicResponse(findCreator(creatorId));
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

        if (request.name() != null) {
            creator.setName(request.name());
        }
        if (request.username() != null && !request.username().equals(creator.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new ApiException(HttpStatus.CONFLICT, "Username is already in use");
            }
            creator.setUsername(request.username());
        }
        if (request.email() != null && !request.email().equals(creator.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new ApiException(HttpStatus.CONFLICT, "Email is already in use");
            }
            creator.setEmail(request.email());
        }
        if (request.phone() != null && !request.phone().equals(creator.getPhone())) {
            userRepository.findByPhone(request.phone()).ifPresent(existing -> {
                if (!existing.getId().equals(creator.getId())) {
                    throw new ApiException(HttpStatus.CONFLICT, "Phone is already in use");
                }
            });
            creator.setPhone(request.phone().isBlank() ? null : request.phone());
        }
        if (request.city() != null) {
            creator.setCity(request.city());
        }
        if (request.avatarUrl() != null) {
            creator.setAvatarUrl(request.avatarUrl());
            creator.setImage(request.avatarUrl());
        }
        if (request.bio() != null) {
            creator.setBio(request.bio());
        }
        if (request.category() != null) {
            creator.setCategory(request.category());
        }
        if (request.coverImageUrl() != null) {
            creator.setCoverImageUrl(request.coverImageUrl());
        }
        if (request.website() != null) {
            creator.setWebsite(request.website());
        }
        if (request.niche() != null) {
            creator.setNiche(request.niche());
        }
        if (request.availabilityStatus() != null) {
            creator.setAvailabilityStatus(request.availabilityStatus());
        }
        if (request.responseTime() != null) {
            creator.setResponseTime(request.responseTime());
        }
        if (request.minPrice() != null) {
            creator.setMinPrice(request.minPrice());
        }
        if (request.maxPrice() != null) {
            creator.setMaxPrice(request.maxPrice());
        }
        if (request.acceptsBarter() != null) {
            creator.setAcceptsBarter(request.acceptsBarter());
        }
        if (request.acceptsHybridDeals() != null) {
            creator.setAcceptsHybridDeals(request.acceptsHybridDeals());
        }
        if (request.minimumBudget() != null) {
            creator.setMinimumBudget(request.minimumBudget());
        }
        if (request.preferredIndustries() != null) {
            creator.setPreferredIndustries(request.preferredIndustries());
        }
        if (request.languages() != null) {
            creator.setLanguages(request.languages());
        }
        if (request.categories() != null) {
            creator.setCategories(request.categories());
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

    public record PayoutPreferencesRequest(
            Boolean autoWithdrawEnabled,
            CreatorPayoutSchedule payoutSchedule,
            Integer minimumPayoutAmount,
            String accountHolderName,
            String ntnNumber,
            String cnicLast4,
            Boolean earningsNotificationsEnabled,
            Boolean weeklyDigestEnabled
    ) {}

    public PaymentSettingsRequest getPaymentSettings(UUID userId, UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can view payment settings");
        Creator creator = findCreator(userId);
        Map<PayoutMethodType, PayoutMethod> methods = new EnumMap<>(PayoutMethodType.class);
        payoutMethodRepository.findByCreatorId(creator.getId()).forEach(method -> methods.put(method.getType(), method));
        PayoutMethod bankTransfer = methods.get(PayoutMethodType.BANK_TRANSFER);
        return new PaymentSettingsRequest(
                accountDetails(methods.get(PayoutMethodType.STCPAY)),
                accountDetails(methods.get(PayoutMethodType.MADA)),
                bankTransfer != null ? bankTransfer.getName() : "",
                bankTransfer != null ? bankTransfer.getAccountDetails() : "",
                accountDetails(methods.get(PayoutMethodType.APPLEPAY)),
                bankTransfer != null ? bankTransfer.getAccountDetails() : ""
        );
    }

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

    @Transactional
    public PayoutPreferencesRequest getPayoutPreferences(UUID userId, UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can view payout preferences");
        Creator creator = findCreator(userId);
        CreatorPayoutPreference prefs = ensurePayoutPreference(creator);
        return new PayoutPreferencesRequest(
                prefs.isAutoWithdrawEnabled(),
                prefs.getPayoutSchedule(),
                prefs.getMinimumPayoutAmount(),
                prefs.getAccountHolderName(),
                prefs.getNtnNumber(),
                prefs.getCnicLast4(),
                prefs.isEarningsNotificationsEnabled(),
                prefs.isWeeklyDigestEnabled()
        );
    }

    @Transactional
    public PayoutPreferencesRequest updatePayoutPreferences(UUID userId, UserRole role, PayoutPreferencesRequest req) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can update payout preferences");
        Creator creator = findCreator(userId);
        CreatorPayoutPreference prefs = ensurePayoutPreference(creator);

        paymentValidationService.validateCnicLast4(req.cnicLast4());

        if (req.autoWithdrawEnabled() != null) prefs.setAutoWithdrawEnabled(req.autoWithdrawEnabled());
        if (req.payoutSchedule() != null) prefs.setPayoutSchedule(req.payoutSchedule());
        if (req.minimumPayoutAmount() != null) prefs.setMinimumPayoutAmount(Math.max(1000, req.minimumPayoutAmount()));
        if (req.accountHolderName() != null) prefs.setAccountHolderName(req.accountHolderName().trim());
        if (req.ntnNumber() != null) prefs.setNtnNumber(req.ntnNumber().trim());
        if (req.cnicLast4() != null) prefs.setCnicLast4(req.cnicLast4().trim());
        if (req.earningsNotificationsEnabled() != null) prefs.setEarningsNotificationsEnabled(req.earningsNotificationsEnabled());
        if (req.weeklyDigestEnabled() != null) prefs.setWeeklyDigestEnabled(req.weeklyDigestEnabled());

        CreatorPayoutPreference saved = creatorPayoutPreferenceRepository.save(prefs);
        paymentAuditService.log(userId, null, "CREATOR_PAYOUT_PREFERENCES_UPDATED", "creator_payout_preferences",
                creator.getId().toString(), "schedule=" + saved.getPayoutSchedule().name().toLowerCase());

        return new PayoutPreferencesRequest(
                saved.isAutoWithdrawEnabled(),
                saved.getPayoutSchedule(),
                saved.getMinimumPayoutAmount(),
                saved.getAccountHolderName(),
                saved.getNtnNumber(),
                saved.getCnicLast4(),
                saved.isEarningsNotificationsEnabled(),
                saved.isWeeklyDigestEnabled()
        );
    }

    private void upsertPayoutMethod(Creator creator, PayoutMethodType type, String name, String accountDetails) {
        if (accountDetails == null || accountDetails.isBlank()) return;
        paymentValidationService.validateCreatorPayoutDetails(type, accountDetails);
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

    private String accountDetails(PayoutMethod method) {
        return method == null ? "" : method.getAccountDetails();
    }

    private CreatorPayoutPreference ensurePayoutPreference(Creator creator) {
        return creatorPayoutPreferenceRepository.findById(creator.getId())
                .orElseGet(() -> creatorPayoutPreferenceRepository.save(CreatorPayoutPreference.builder()
                        .creator(creator)
                        .build()));
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

    public List<CreatorResponse> getTrending(int limit) {
        Pageable pageable = cappedPageable(0, limit, "createdAt");
        return creatorRepository.findByIsTrendingTrue(pageable).stream()
                .map(creatorMapper::toPublicResponse)
                .toList();
    }

    public List<CreatorResponse> getBarterFriendly(int limit) {
        Pageable pageable = cappedPageable(0, limit, "createdAt");
        return creatorRepository.findByAcceptsBarterTrue(pageable).stream()
                .map(creatorMapper::toPublicResponse)
                .toList();
    }

    public List<CreatorResponse> getFastResponders(int limit) {
        Pageable pageable = cappedPageable(0, limit, "createdAt");
        return creatorRepository.findByIsFastResponderTrue(pageable).stream()
                .map(creatorMapper::toPublicResponse)
                .toList();
    }

    public List<CreatorResponse> getByCity(String city, int limit) {
        Pageable pageable = cappedPageable(0, limit, "createdAt");
        return creatorRepository.findByCityIgnoreCaseAndActiveTrue(city, pageable).stream()
                .map(creatorMapper::toPublicResponse)
                .toList();
    }

    private Pageable cappedPageable(int page, int limit, String sortField) {
        return cappedPageable(page, limit, sortField, Sort.Direction.DESC);
    }

    private Pageable cappedPageable(int page, int limit, String sortField, Sort.Direction direction) {
        int safePage = Math.max(page, 0);
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return PageRequest.of(safePage, safeLimit, Sort.by(direction, sortField));
    }
}
