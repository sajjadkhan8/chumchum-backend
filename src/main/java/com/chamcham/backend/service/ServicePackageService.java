package com.chamcham.backend.service;

import com.chamcham.backend.dto.servicepackage.ServicePackageCreateRequest;
import com.chamcham.backend.entity.PackageAnalytics;
import com.chamcham.backend.entity.PackageTier;
import com.chamcham.backend.dto.servicepackage.ServicePackageResponse;
import com.chamcham.backend.entity.Package;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.PackageStatus;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.ServicePackageMapper;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.PackageAnalyticsRepository;
import com.chamcham.backend.repository.ServicePackageRepository;
import com.chamcham.backend.util.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

    private static final Map<PackageStatus, Set<PackageStatus>> STATUS_TRANSITIONS = Map.of(
            PackageStatus.DRAFT,        EnumSet.of(PackageStatus.ACTIVE, PackageStatus.ARCHIVED),
            PackageStatus.ACTIVE,       EnumSet.of(PackageStatus.PAUSED, PackageStatus.ARCHIVED),
            PackageStatus.PAUSED,       EnumSet.of(PackageStatus.ACTIVE, PackageStatus.ARCHIVED),
            PackageStatus.ARCHIVED,     EnumSet.noneOf(PackageStatus.class),
            PackageStatus.UNDER_REVIEW, EnumSet.noneOf(PackageStatus.class)
    );

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "price", "ordersCompleted"
    );

    public ServicePackageService(
            ServicePackageRepository servicePackageRepository,
            CreatorRepository creatorRepository,
            ServicePackageMapper servicePackageMapper,
            PackageAnalyticsRepository packageAnalyticsRepository
    ) {
        this.servicePackageRepository = servicePackageRepository;
        this.creatorRepository = creatorRepository;
        this.servicePackageMapper = servicePackageMapper;
        this.packageAnalyticsRepository = packageAnalyticsRepository;
    }

    public ServicePackageResponse createPackage(UUID userId, UserRole role, ServicePackageCreateRequest request) {
        if (!role.isCreator() && !role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can create packages!");
        }

        validatePricing(request);

        Creator creator = creatorRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Creator profile not found for this user"));

        String packageName = request.name().trim();
        if (servicePackageRepository.existsByCreatorAndNameIgnoreCase(creator, packageName)) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a package with this name");
        }

        Package aPackage = Package.builder()
                .creator(creator)
                .name(packageName)
                .title(request.title())
                .description(request.description())
                .platform(request.platform())
                .category(request.category())
                .type(request.type())
                .shortDescription(request.shortDescription())
                .fullDescription(request.fullDescription())
                .dealType(request.dealType() == null ? DealType.PAID : request.dealType())
                .barterDetails(request.barterDetails())
                .barterDescription(request.barterDescription())
                .barterCategory(request.barterCategory())
                .estimatedBarterValue(request.estimatedBarterValue())
                .hybridCashAmount(request.hybridCashAmount())
                .hybridBarterValue(request.hybridBarterValue())
                .creatorExpectations(request.creatorExpectations())
                .price(request.price())
                .currency(request.currency() == null || request.currency().isBlank() ? "SAR" : request.currency())
                .deliverables(request.deliverables())
                .deliveryDays(request.deliveryDays())
                .revisions(request.revisions() == null ? 1 : request.revisions())
                .status(request.status() == null ? PackageStatus.DRAFT : request.status())
                .visibility(request.visibility() == null || request.visibility().isBlank() ? "public" : request.visibility())
                .responseTime(request.responseTime())
                .featured(request.isFeatured() != null && request.isFeatured())
                .mediaUrls(toArray(request.mediaUrls()))
                .tags(request.tags())
                .active(request.isActive() == null || request.isActive())
                .coverImage(request.coverImage())
                .build();

        if (request.tiers() != null && !request.tiers().isEmpty()) {
            List<PackageTier> tiers = request.tiers().stream()
                    .map(tier -> PackageTier.builder()
                            .aPackage(aPackage)
                            .name(tier.name())
                            .price(tier.price())
                            .deliverables(tier.deliverables())
                            .deliveryDays(tier.deliveryDays())
                            .revisions(tier.revisions() == null ? 1 : tier.revisions())
                            .build())
                    .toList();
            aPackage.setTiers(tiers);
        }

        return servicePackageMapper.toResponse(servicePackageRepository.save(aPackage));
    }

    public void deletePackage(UUID packageId, UUID userId, UserRole role) {
        Package aPackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));

        if (!role.isAdmin() && !aPackage.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid request! Cannot delete other creator packages!");
        }

        servicePackageRepository.delete(aPackage);
    }

    public ServicePackageResponse updatePackage(UUID packageId, UUID userId, UserRole role,
                                                ServicePackageCreateRequest request) {
        Package pkg = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));

        if (!role.isAdmin() && !pkg.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot update another creator's package");
        }

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
        if (request.barterCategory() != null) pkg.setBarterCategory(request.barterCategory());
        if (request.estimatedBarterValue() != null) pkg.setEstimatedBarterValue(request.estimatedBarterValue());
        if (request.hybridCashAmount() != null) pkg.setHybridCashAmount(request.hybridCashAmount());
        if (request.hybridBarterValue() != null) pkg.setHybridBarterValue(request.hybridBarterValue());
        if (request.creatorExpectations() != null) pkg.setCreatorExpectations(request.creatorExpectations());
        if (request.deliverables() != null && !request.deliverables().isEmpty()) pkg.setDeliverables(request.deliverables());
        if (request.deliveryDays() != null) pkg.setDeliveryDays(request.deliveryDays());
        if (request.revisions() != null) pkg.setRevisions(request.revisions());
        if (request.visibility() != null) pkg.setVisibility(request.visibility());
        if (request.responseTime() != null) pkg.setResponseTime(request.responseTime());
        if (request.coverImage() != null) pkg.setCoverImage(request.coverImage());
        if (request.tags() != null) pkg.setTags(request.tags());
        if (request.mediaUrls() != null) pkg.setMediaUrls(toArray(request.mediaUrls()));
        if (request.isFeatured() != null) pkg.setFeatured(request.isFeatured());
        if (request.isActive() != null) pkg.setActive(request.isActive());

        return servicePackageMapper.toResponse(servicePackageRepository.save(pkg));
    }

    public ServicePackageResponse updateStatus(UUID packageId, UUID userId, UserRole role, String rawStatus) {
        Package pkg = servicePackageRepository.findById(packageId)
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

        pkg.setStatus(newStatus);
        return servicePackageMapper.toResponse(servicePackageRepository.save(pkg));
    }

    public ServicePackageResponse duplicate(UUID packageId, UUID userId, UserRole role) {
        Package src = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));

        if (!role.isAdmin() && !src.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot duplicate another creator's package");
        }

        Package copy = Package.builder()
                .creator(src.getCreator())
                .name("Copy of " + src.getName())
                .title("Copy of " + src.getTitle())
                .shortDescription(src.getShortDescription())
                .description(src.getDescription())
                .fullDescription(src.getFullDescription())
                .platform(src.getPlatform())
                .category(src.getCategory())
                .type(src.getType())
                .dealType(src.getDealType())
                .status(PackageStatus.DRAFT)
                .visibility("public")
                .price(src.getPrice())
                .currency(src.getCurrency())
                .barterDetails(src.getBarterDetails())
                .barterCategory(src.getBarterCategory())
                .deliverables(src.getDeliverables())
                .deliveryDays(src.getDeliveryDays())
                .revisions(src.getRevisions())
                .tags(src.getTags())
                .coverImage(src.getCoverImage())
                .build();

        return servicePackageMapper.toResponse(servicePackageRepository.save(copy));
    }

    public Map<String, Object> getAnalytics(UUID packageId, UUID userId, UserRole role) {
        Package pkg = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));

        if (!role.isAdmin() && !pkg.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot view analytics for another creator's package");
        }

        PackageAnalytics analytics = packageAnalyticsRepository.findByServicePackageId(packageId)
                .orElse(PackageAnalytics.builder().aPackage(pkg).build());

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

    public ServicePackageResponse getPackage(UUID packageId) {
        Package aPackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found!"));
        return servicePackageMapper.toResponse(aPackage);
    }

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

        Page<Package> packages = servicePackageRepository.findFeaturedForFeed(pageable);
        return PageResponse.from(packages.map(servicePackageMapper::toResponse));
    }

    public PageResponse<ServicePackageResponse> getPackages(
            String category,
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
        Page<Package> packages;

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
}
