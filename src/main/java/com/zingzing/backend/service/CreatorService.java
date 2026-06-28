package com.zingzing.backend.service;

import com.zingzing.backend.dto.creator.CreatorCreateRequest;
import com.zingzing.backend.dto.creator.CreatorResponse;
import com.zingzing.backend.dto.creator.CreatorUpdateRequest;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.CreatorPayoutPreference;
import com.zingzing.backend.entity.PayoutMethod;
import com.zingzing.backend.entity.SocialAccount;
import com.zingzing.backend.entity.User;
import com.zingzing.backend.entity.enums.AvailabilityStatus;
import com.zingzing.backend.entity.enums.CreatorBadgeLevel;
import com.zingzing.backend.entity.enums.CreatorPayoutSchedule;
import com.zingzing.backend.entity.enums.PackageCategory;
import com.zingzing.backend.entity.enums.PackagePlatform;
import com.zingzing.backend.entity.enums.PayoutMethodType;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.mapper.CreatorMapper;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.CreatorPayoutPreferenceRepository;
import com.zingzing.backend.repository.PayoutMethodRepository;
import com.zingzing.backend.repository.SocialAccountRepository;
import com.zingzing.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CreatorService {

    private static final Set<String> SUPPORTED_SOCIAL_PLATFORMS = Arrays.stream(PackagePlatform.values())
            .map(p -> p.name().toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toSet());
    private static final Set<String> SUPPORTED_COLLABORATION_PREFERENCES = Set.of("paid", "barter", "hybrid");

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

        saveSocialAccountsFromUrls(created,
                request.instagramUrl(), request.tiktokUrl(),
                request.youtubeUrl(), request.facebookUrl(), request.snapchatUrl());

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

    private static String toJsonArray(List<String> items) {
        if (items == null || items.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"");
            sb.append(items.get(i).replace("\\", "\\\\").replace("\"", "\\\""));
            sb.append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<String> normalizeCollaborationPreferences(List<String> preferences, boolean requireValue, boolean includePaidByDefault) {
        if (preferences == null) return requireValue ? List.of("paid") : null;
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (includePaidByDefault) normalized.add("paid");
        for (String preference : preferences) {
            if (preference == null || preference.isBlank()) continue;
            String value = preference.trim().toLowerCase(Locale.ROOT);
            if (!SUPPORTED_COLLABORATION_PREFERENCES.contains(value)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported collaboration preference: " + preference);
            }
            normalized.add(value);
        }
        if (requireValue && normalized.isEmpty()) normalized.add("paid");
        return new ArrayList<>(normalized);
    }

    public CreatorSearchResult search(
            String search, List<String> cities,
            List<String> categories, List<String> languages,
            Integer minFollowers, Integer maxFollowers,
            BigDecimal minRating, Integer minPrice, Integer maxPrice,
            Integer minReviews,
            CreatorBadgeLevel badgeLevel, AvailabilityStatus availabilityStatus,
            List<String> collaborationPreferences, Boolean isTrending, Boolean isFastResponder,
            Boolean ambassadorOnly, String platform, BigDecimal minEngagementRate,
            Integer minCompletionRate,
            Integer maxRateCardReel, Integer maxRateCardStory,
            Integer maxRateCardPost, Integer maxRateCardVideo,
            int page, int limit, String sortBy) {

        String normalizedSort = sortBy == null ? "" : sortBy.trim().toLowerCase();
        String safeSort = switch (normalizedSort) {
            case "trending", "top_rated", "budget_friendly", "budget_high" -> normalizedSort;
            default -> "created_at";
        };

        Boolean isVerified = Boolean.TRUE.equals(ambassadorOnly) ? Boolean.TRUE : null;

        int safePage = Math.max(page, 0);
        int safeLimit = Math.clamp(limit, 1, 100);
        Pageable pageable = PageRequest.of(safePage, safeLimit);
        List<String> normalizedCategories = categories == null ? null : PackageCategory.normalizeCreatorCategories(categories);

        Page<Creator> result = creatorRepository.search(
                search, toJsonArray(cities),
                toJsonArray(normalizedCategories == null || normalizedCategories.isEmpty() ? null : normalizedCategories),
                toJsonArray(languages),
                minFollowers, maxFollowers, minRating,
                minPrice, maxPrice, minReviews,
                badgeLevel != null ? badgeLevel.name() : null,
                availabilityStatus != null ? availabilityStatus.name() : null,
                toJsonArray(normalizeCollaborationPreferences(collaborationPreferences, false, false)), isTrending, isFastResponder,
                isVerified, minEngagementRate,
                platform == null || platform.isBlank() ? null : platform.trim().toLowerCase(),
                minCompletionRate,
                maxRateCardReel, maxRateCardStory, maxRateCardPost, maxRateCardVideo,
                safeSort,
                pageable);

        return new CreatorSearchResult(
                result.getContent().stream().map(creatorMapper::toPublicResponse).toList(),
                result.getTotalElements(), page, limit);
    }

    @Transactional
    public CreatorResponse getById(UUID creatorId) {
        return creatorMapper.toPublicResponse(findCreator(creatorId));
    }

    public CreatorResponse getByUsername(String username) {
        Creator creator = creatorRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
        return creatorMapper.toPublicResponse(creator);
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
        if (request.coverImageUrl() != null) {
            creator.setCoverImageUrl(request.coverImageUrl());
        }
        if (request.website() != null) {
            creator.setWebsite(request.website());
        }
        if (request.availabilityStatus() != null) {
            creator.setAvailabilityStatus(request.availabilityStatus());
        }
        if (request.isFiler() != null) {
            creator.setFiler(request.isFiler());
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
        if (request.minimumBudget() != null) {
            creator.setMinimumBudget(request.minimumBudget());
        }
        if (request.languages() != null) {
            creator.setLanguages(request.languages());
        }
        if (request.categories() != null) {
            List<String> normalizedCategories = PackageCategory.normalizeCreatorCategories(request.categories());
            if (normalizedCategories.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Select at least one content category for your public profile");
            }
            creator.setCategories(normalizedCategories);
        }
        saveSocialAccountsFromUrls(creator,
                request.instagramUrl(), request.tiktokUrl(),
                request.youtubeUrl(), request.facebookUrl(), request.snapchatUrl());
        if (request.rateCardReel() != null) creator.setRateCardReel(request.rateCardReel());
        if (request.rateCardStory() != null) creator.setRateCardStory(request.rateCardStory());
        if (request.rateCardPost() != null) creator.setRateCardPost(request.rateCardPost());
        if (request.rateCardVideo() != null) creator.setRateCardVideo(request.rateCardVideo());
        if (request.collaborationPreferences() != null) {
            creator.setCollaborationPreferences(normalizeCollaborationPreferences(request.collaborationPreferences(), true, true));
        }
        if (request.barterTypes() != null) creator.setBarterTypes(request.barterTypes());

        return creatorMapper.toResponse(creatorRepository.save(creator));
    }

    // ---- Social accounts ----

    private void saveSocialAccountsFromUrls(Creator creator,
                                             String instagramUrl, String tiktokUrl,
                                             String youtubeUrl, String facebookUrl, String snapchatUrl) {
        Set<String> existing = socialAccountRepository.findByCreatorId(creator.getId()).stream()
                .map(sa -> sa.getPlatform().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        List<SocialAccount> toSave = new ArrayList<>();
        addFromUrl(toSave, existing, creator, PackagePlatform.INSTAGRAM.name().toLowerCase(Locale.ROOT), instagramUrl);
        addFromUrl(toSave, existing, creator, PackagePlatform.TIKTOK.name().toLowerCase(Locale.ROOT), tiktokUrl);
        addFromUrl(toSave, existing, creator, PackagePlatform.YOUTUBE.name().toLowerCase(Locale.ROOT), youtubeUrl);
        addFromUrl(toSave, existing, creator, PackagePlatform.FACEBOOK.name().toLowerCase(Locale.ROOT), facebookUrl);
        addFromUrl(toSave, existing, creator, PackagePlatform.SNAPCHAT.name().toLowerCase(Locale.ROOT), snapchatUrl);
        if (!toSave.isEmpty()) socialAccountRepository.saveAll(toSave);
    }

    private void addFromUrl(List<SocialAccount> list, Set<String> existing,
                             Creator creator, String platform, String url) {
        if (url == null || url.isBlank() || existing.contains(platform)) return;
        String path = url.replaceAll("/$", "");
        int slash = path.lastIndexOf('/');
        String raw = slash >= 0 ? path.substring(slash + 1) : path;
        String username = raw.startsWith("@") ? raw.substring(1) : raw;
        list.add(SocialAccount.builder()
                .creator(creator)
                .platform(platform)
                .username(username.isBlank() ? "unknown" : username)
                .profileUrl(url)
                .followers(0)
                .engagementRate(BigDecimal.ZERO)
                .build());
    }

    public record SocialAccountRequest(
            String platform, String username, String profileUrl,
            Integer followers, Integer avgViews, BigDecimal engagementRate) {}

    @Transactional
    public List<SocialAccount> updateSocialAccounts(UUID userId, UserRole role, List<SocialAccountRequest> accounts) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can update social accounts");
        Creator creator = findCreator(userId);
        Set<String> platforms = new LinkedHashSet<>();
        List<SocialAccountRequest> normalizedAccounts = accounts.stream()
                .map(a -> new SocialAccountRequest(
                        normalizeSocialPlatform(a.platform()),
                        a.username(),
                        a.profileUrl(),
                        a.followers(),
                        a.avgViews(),
                        a.engagementRate()))
                .toList();
        for (SocialAccountRequest account : normalizedAccounts) {
            if (!platforms.add(account.platform())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Each social platform can only be added once");
            }
        }

        // delete existing and re-insert (simpler than upsert per-platform)
        socialAccountRepository.deleteAll(socialAccountRepository.findByCreatorId(userId));
        List<SocialAccount> saved = normalizedAccounts.stream().map(a -> socialAccountRepository.save(
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

    public record SocialAccountPatchRequest(
            String username, String profileUrl,
            Integer followers, Integer avgViews, BigDecimal engagementRate) {}

    @Transactional
    public SocialAccount patchSocialAccount(UUID userId, String platform, SocialAccountPatchRequest req) {
        Creator creator = findCreator(userId);
        String normalizedPlatform = normalizeSocialPlatform(platform);
        SocialAccount account = socialAccountRepository
                .findByCreatorIdAndPlatformIgnoreCase(userId, normalizedPlatform)
                .orElseGet(() -> SocialAccount.builder()
                        .creator(creator)
                        .platform(normalizedPlatform)
                        .username("")
                        .followers(0)
                        .engagementRate(BigDecimal.ZERO)
                        .build());
        if (req.username() != null) account.setUsername(req.username());
        if (req.profileUrl() != null) account.setProfileUrl(req.profileUrl());
        if (req.followers() != null) account.setFollowers(req.followers());
        if (req.avgViews() != null) account.setAvgViews(req.avgViews());
        if (req.engagementRate() != null) account.setEngagementRate(req.engagementRate());
        return socialAccountRepository.save(account);
    }

    @Transactional
    public void deleteSocialAccount(UUID userId, UserRole role, String platform) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can update social accounts");
        String normalizedPlatform = normalizeSocialPlatform(platform);
        socialAccountRepository.deleteByCreatorIdAndPlatform(userId, normalizedPlatform);
    }

    private String normalizeSocialPlatform(String platform) {
        String normalized = platform == null ? "" : platform.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_SOCIAL_PLATFORMS.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported social platform");
        }
        return normalized;
    }

    // ---- Preferences ----

    public record PreferencesRequest(
            List<String> collaborationPreferences,
            Integer minimumBudget) {}

    @Transactional
    public CreatorResponse updatePreferences(UUID userId, UserRole role, PreferencesRequest req) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can update preferences");
        Creator creator = findCreator(userId);
        if (req.collaborationPreferences() != null) {
            creator.setCollaborationPreferences(normalizeCollaborationPreferences(req.collaborationPreferences(), true, true));
        }
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
                maskAccount(accountDetails(methods.get(PayoutMethodType.STCPAY))),
                maskAccount(accountDetails(methods.get(PayoutMethodType.MADA))),
                bankTransfer != null ? bankTransfer.getName() : "",
                maskAccount(bankTransfer != null ? bankTransfer.getAccountDetails() : ""),
                maskAccount(accountDetails(methods.get(PayoutMethodType.APPLEPAY))),
                maskAccount(bankTransfer != null ? bankTransfer.getAccountDetails() : "")
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
                maskNtn(prefs.getNtnNumber()),
                null,  // cnicLast4 is write-only; never returned
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
                maskNtn(saved.getNtnNumber()),
                null,  // cnicLast4 is write-only; never returned
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

    private String maskAccount(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        if (raw.length() < 4) return "****";
        return "*".repeat(raw.length() - 4) + raw.substring(raw.length() - 4);
    }

    private String maskNtn(String ntn) {
        if (ntn == null || ntn.isBlank()) return ntn;
        if (ntn.length() < 3) return "***";
        return "*".repeat(ntn.length() - 3) + ntn.substring(ntn.length() - 3);
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

    public List<CreatorResponse> getTrending(int limit) {
        Pageable pageable = cappedPageable(0, limit, "createdAt");
        return creatorRepository.findByIsTrendingTrue(pageable).stream()
                .map(creatorMapper::toPublicResponse)
                .toList();
    }

    public List<CreatorResponse> getBarterFriendly(int limit) {
        Pageable pageable = cappedPageable(0, limit, "createdAt");
        return creatorRepository.findByBarterCollaborationPreference(pageable).stream()
                .map(creatorMapper::toPublicResponse)
                .toList();
    }

    public List<CreatorResponse> getFastResponders(int limit) {
        Pageable pageable = cappedPageable(0, limit, "createdAt");
        return creatorRepository.findByIsFastResponderTrue(pageable).stream()
                .map(creatorMapper::toPublicResponse)
                .toList();
    }

    public List<CreatorResponse> getRisingStars(int limit) {
        Pageable pageable = cappedPageable(0, limit, "engagementRate");
        return creatorRepository.findRisingStars(pageable).stream()
                .map(creatorMapper::toPublicResponse)
                .toList();
    }

    public List<CreatorResponse> getVerified(int limit) {
        Pageable pageable = cappedPageable(0, limit, "rating");
        return creatorRepository.findByIsVerifiedTrue(pageable).getContent().stream()
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
