package com.zingzing.backend.service;

import com.zingzing.backend.dto.servicepackage.ServicePackageCreateRequest;
import com.zingzing.backend.entity.PackageAnalytics;
import com.zingzing.backend.dto.servicepackage.ServicePackageResponse;
import com.zingzing.backend.entity.ServicePackage;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.enums.DealType;
import com.zingzing.backend.entity.enums.PackageCategory;
import com.zingzing.backend.entity.enums.PackageStatus;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.mapper.ServicePackageMapper;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.PackageAnalyticsRepository;
import com.zingzing.backend.repository.ServicePackageRepository;
import com.zingzing.backend.util.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ServicePackageService {

    private final ServicePackageRepository servicePackageRepository;
    private final CreatorRepository creatorRepository;
    private final ServicePackageMapper servicePackageMapper;
    private final PackageAnalyticsRepository packageAnalyticsRepository;
    private final PackageAnalyticsTrackingService packageAnalyticsTrackingService;

    private static final Map<PackageStatus, Set<PackageStatus>> STATUS_TRANSITIONS = Map.of(
            PackageStatus.DRAFT,        EnumSet.of(PackageStatus.ACTIVE, PackageStatus.ARCHIVED),
            PackageStatus.ACTIVE,       EnumSet.of(PackageStatus.PAUSED, PackageStatus.ARCHIVED),
            PackageStatus.PAUSED,       EnumSet.of(PackageStatus.ACTIVE, PackageStatus.ARCHIVED),
            PackageStatus.ARCHIVED,     EnumSet.noneOf(PackageStatus.class)
    );

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "price", "ordersCompleted"
    );

    public ServicePackageService(
            ServicePackageRepository servicePackageRepository,
            CreatorRepository creatorRepository,
            ServicePackageMapper servicePackageMapper,
            PackageAnalyticsRepository packageAnalyticsRepository,
            PackageAnalyticsTrackingService packageAnalyticsTrackingService
    ) {
        this.servicePackageRepository = servicePackageRepository;
        this.creatorRepository = creatorRepository;
        this.servicePackageMapper = servicePackageMapper;
        this.packageAnalyticsRepository = packageAnalyticsRepository;
        this.packageAnalyticsTrackingService = packageAnalyticsTrackingService;
    }

    @Transactional
    public ServicePackageResponse createPackage(UUID userId, UserRole role, ServicePackageCreateRequest request) {
        if (!role.isCreator() && !role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can create packages!");
        }

        validatePricing(request);

        Creator creator = creatorRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Creator profile not found for this user"));
        PackageStatus targetStatus = request.status() == null ? PackageStatus.DRAFT : request.status();
        validatePublishable(targetStatus, request.category());

        String packageName = request.name().trim();
        if (servicePackageRepository.existsByCreatorAndNameIgnoreCase(creator, packageName)) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a package with this name");
        }

        ServicePackage servicePackage = ServicePackage.builder()
                .creator(creator)
                .name(packageName)
                .title(request.title())
                .description(request.description())
                .platform(request.platform())
                .category(request.category())
                .shortDescription(request.shortDescription())
                .fullDescription(request.fullDescription())
                .dealType(request.dealType() == null ? DealType.PAID : request.dealType())
                .barterDetails(request.barterDetails())
                .barterDescription(request.barterDescription())
                .hybridCashAmount(request.hybridCashAmount())
                .creatorExpectations(request.creatorExpectations())
                .price(request.price())
                .currency("PKR")  // V1: PKR only (mono-currency)
                .deliverables(request.deliverables())
                .deliveryDays(request.deliveryDays())
                .revisions(request.revisions() == null ? 1 : request.revisions())
                .status(targetStatus)
                .visibility(request.visibility() == null || request.visibility().isBlank() ? "public" : request.visibility())
                .featured(request.isFeatured() != null && request.isFeatured())
                .mediaUrls(toArray(request.mediaUrls()))
                .tags(request.tags())
                .active(request.isActive() == null || request.isActive())
                .coverImage(request.coverImage())
                .build();

        clearIncompatiblePricingFields(servicePackage);
        syncCreatorCategoryForPublishedPackage(creator, servicePackage.getStatus(), servicePackage.getCategory());

        return servicePackageMapper.toResponse(servicePackageRepository.save(servicePackage));
    }

    @Transactional
    public void deletePackage(UUID packageId, UUID userId, UserRole role) {
        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));

        if (!role.isAdmin() && !servicePackage.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid request! Cannot delete other creator packages!");
        }

        servicePackageRepository.delete(servicePackage);
    }

    @Transactional
    public ServicePackageResponse updatePackage(UUID packageId, UUID userId, UserRole role,
                                                ServicePackageCreateRequest request) {
        ServicePackage pkg = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));

        if (!role.isAdmin() && !pkg.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot update another creator's package");
        }

        validatePricing(request);

        if (request.title() != null) pkg.setTitle(request.title());
        if (request.name() != null)  pkg.setName(request.name().trim());
        if (request.shortDescription() != null) pkg.setShortDescription(request.shortDescription());
        if (request.description() != null)  pkg.setDescription(request.description());
        if (request.fullDescription() != null) pkg.setFullDescription(request.fullDescription());
        if (request.platform() != null) pkg.setPlatform(request.platform());
        if (request.category() != null) pkg.setCategory(request.category());
        if (request.dealType() != null) pkg.setDealType(request.dealType());
        if (request.price() != null) pkg.setPrice(request.price());
        if (request.barterDetails() != null) pkg.setBarterDetails(request.barterDetails());
        if (request.barterDescription() != null) pkg.setBarterDescription(request.barterDescription());
        if (request.hybridCashAmount() != null) pkg.setHybridCashAmount(request.hybridCashAmount());
        if (request.creatorExpectations() != null) pkg.setCreatorExpectations(request.creatorExpectations());
        if (request.deliverables() != null && !request.deliverables().isEmpty()) pkg.setDeliverables(request.deliverables());
        if (request.deliveryDays() != null) pkg.setDeliveryDays(request.deliveryDays());
        if (request.revisions() != null) pkg.setRevisions(request.revisions());
        if (request.visibility() != null) pkg.setVisibility(request.visibility());
        if (request.coverImage() != null) pkg.setCoverImage(request.coverImage());
        if (request.tags() != null) pkg.setTags(request.tags());
        if (request.mediaUrls() != null) pkg.setMediaUrls(toArray(request.mediaUrls()));
        if (request.isFeatured() != null) pkg.setFeatured(request.isFeatured());
        if (request.isActive() != null) pkg.setActive(request.isActive());
        if (request.status() != null) pkg.setStatus(request.status());

        clearIncompatiblePricingFields(pkg);
        validatePublishable(pkg.getStatus(), pkg.getCategory());
        syncCreatorCategoryForPublishedPackage(pkg.getCreator(), pkg.getStatus(), pkg.getCategory());

        return servicePackageMapper.toResponse(servicePackageRepository.save(pkg));
    }

    @Transactional
    public ServicePackageResponse updateStatus(UUID packageId, UUID userId, UserRole role, String rawStatus) {
        ServicePackage pkg = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));

        if (!role.isAdmin() && !pkg.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot update another creator's package status");
        }

        PackageStatus newStatus;
        try {
            newStatus = PackageStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status: " + rawStatus);
        }

        Set<PackageStatus> allowed = STATUS_TRANSITIONS.getOrDefault(pkg.getStatus(), EnumSet.noneOf(PackageStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot transition from " + pkg.getStatus() + " to " + newStatus);
        }

        validatePublishable(newStatus, pkg.getCategory());
        pkg.setStatus(newStatus);
        syncCreatorCategoryForPublishedPackage(pkg.getCreator(), pkg.getStatus(), pkg.getCategory());
        return servicePackageMapper.toResponse(servicePackageRepository.save(pkg));
    }

    @Transactional
    public ServicePackageResponse duplicate(UUID packageId, UUID userId, UserRole role) {
        ServicePackage src = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));

        if (!role.isAdmin() && !src.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot duplicate another creator's package");
        }

        ServicePackage copy = ServicePackage.builder()
                .creator(src.getCreator())
                .name("Copy of " + src.getName())
                .title("Copy of " + src.getTitle())
                .shortDescription(src.getShortDescription())
                .description(src.getDescription())
                .fullDescription(src.getFullDescription())
                .platform(src.getPlatform())
                .category(src.getCategory())
                .dealType(src.getDealType())
                .status(PackageStatus.DRAFT)
                .visibility("public")
                .price(src.getPrice())
                .currency(src.getCurrency())
                .barterDetails(src.getBarterDetails())
                .barterDescription(src.getBarterDescription())
                .hybridCashAmount(src.getHybridCashAmount())
                .creatorExpectations(src.getCreatorExpectations())
                .deliverables(src.getDeliverables())
                .deliveryDays(src.getDeliveryDays())
                .revisions(src.getRevisions())
                .tags(src.getTags())
                .coverImage(src.getCoverImage())
                .mediaUrls(src.getMediaUrls())
                .build();

        return servicePackageMapper.toResponse(servicePackageRepository.save(copy));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics(UUID packageId, UUID userId, UserRole role) {
        ServicePackage pkg = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));

        if (!role.isAdmin() && !pkg.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot view analytics for another creator's package");
        }

        PackageAnalytics analytics = packageAnalyticsRepository.findByServicePackageId(packageId)
                .orElse(PackageAnalytics.builder().servicePackage(pkg).build());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("views", analytics.getViews());
        data.put("clicks", analytics.getClicks());
        data.put("inquiries", analytics.getInquiries());
        data.put("conversionRate", analytics.getConversionRate());
        data.put("completionRate", analytics.getCompletionRate());
        data.put("repeatBrands", analytics.getRepeatBrands());
        data.put("engagementPerformance", analytics.getEngagementPerformance());
        return Map.of("success", true, "data", data);
    }

    @Transactional
    public ServicePackageResponse getPackage(UUID packageId) {
        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found!"));
        packageAnalyticsTrackingService.track(servicePackage, "VIEW", null, null, "package_detail", "{}");
        return servicePackageMapper.toResponse(servicePackage);
    }

    @Transactional
    public void trackPackageEvent(UUID packageId, String eventType, String source) {
        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found!"));
        packageAnalyticsTrackingService.track(servicePackage, eventType, null, null, source, "{}");
    }

    public PageResponse<ServicePackageResponse> getMyPackages(UUID userId, UserRole role,
            int page, int size, String sort,
            String search, String status, String dealType, String platform) {
        if (!role.isCreator() && !role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can view their packages");
        }
        String safeSort = ALLOWED_SORT_FIELDS.contains(sort) ? sort : "createdAt";
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, safeSort));

        PackageStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try { parsedStatus = PackageStatus.valueOf(status.trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        DealType parsedDealType = null;
        if (dealType != null && !dealType.isBlank()) {
            try { parsedDealType = DealType.valueOf(dealType.trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        String platformParam = (platform != null && !platform.isBlank()) ? platform.trim() : null;

        Page<ServicePackage> pkgPage = servicePackageRepository.findByCreatorIdFiltered(
                userId, parsedStatus, parsedDealType, platformParam, searchParam, pageable);

        return PageResponse.from(pkgPage.map(servicePackageMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ServicePackageResponse> getFeaturedPackages(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Order.desc("ordersCompleted"),
                        Sort.Order.desc("createdAt")
                )
        );

        Page<ServicePackage> packages = servicePackageRepository.findFeaturedForFeed(pageable);
        return PageResponse.from(packages.map(servicePackageMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ServicePackageResponse> getPackages(
            PackageCategory category,
            String search,
            Integer min,
            Integer max,
            UUID creatorId,
            UUID creatorUserId,
            int page,
            int size,
            String sortBy
    ) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, safeSortBy));
        Page<ServicePackage> packages;

        if (creatorId != null || creatorUserId != null) {
            UUID resolvedCreatorId = creatorId != null ? creatorId : creatorUserId;
            Creator creator = creatorRepository.findById(resolvedCreatorId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
            packages = servicePackageRepository.findByCreator(creator, pageable);
        } else {
            packages = servicePackageRepository.searchActive(
                    category,
                    search,
                    min,
                    max,
                    pageable
            );
        }

        return PageResponse.from(packages.map(servicePackageMapper::toResponse));
    }

    private String[] toArray(List<String> values) {
        return values == null || values.isEmpty() ? null : values.toArray(String[]::new);
    }

    private void validatePricing(ServicePackageCreateRequest request) {
        DealType dealType = request.dealType() == null ? DealType.PAID : request.dealType();

        if (dealType == DealType.BARTER
                && (request.barterDetails() == null || request.barterDetails().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "barterDetails is required when dealType is BARTER");
        }

        if ((dealType == DealType.PAID || dealType == DealType.HYBRID)
                && (request.price() == null || request.price() <= 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "price is required when dealType is PAID/HYBRID");
        }

    }

    private void validatePublishable(PackageStatus status, PackageCategory category) {
        if (!requiresPublishReadyCategory(status)) {
            return;
        }
        if (category == null || category == PackageCategory.QUICK_DEAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a package category before publishing");
        }
    }

    private boolean requiresPublishReadyCategory(PackageStatus status) {
        return status == PackageStatus.ACTIVE;
    }

    private void syncCreatorCategoryForPublishedPackage(Creator creator, PackageStatus status, PackageCategory category) {
        if (!requiresPublishReadyCategory(status) || creator == null || category == null || category == PackageCategory.QUICK_DEAL) {
            return;
        }

        List<String> normalizedCategories = PackageCategory.normalizeCreatorCategories(creator.getCategories());
        String categoryValue = category.name();
        if (!normalizedCategories.contains(categoryValue)) {
            normalizedCategories = new java.util.ArrayList<>(normalizedCategories);
            normalizedCategories.add(categoryValue);
            creator.setCategories(normalizedCategories);
            creatorRepository.save(creator);
        } else if (!normalizedCategories.equals(creator.getCategories())) {
            creator.setCategories(normalizedCategories);
            creatorRepository.save(creator);
        }
    }

    private void clearIncompatiblePricingFields(ServicePackage pkg) {
        DealType dealType = pkg.getDealType() == null ? DealType.PAID : pkg.getDealType();

        if (dealType == DealType.PAID) {
            pkg.setBarterDetails(null);
            pkg.setBarterDescription(null);
            pkg.setHybridCashAmount(null);
            pkg.setCreatorExpectations(null);
            return;
        }

        if (dealType == DealType.BARTER) {
            pkg.setHybridCashAmount(null);
            pkg.setPrice(0);
        }
    }
}
