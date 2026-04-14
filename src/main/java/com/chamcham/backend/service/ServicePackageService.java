package com.chamcham.backend.service;

import com.chamcham.backend.dto.servicepackage.ServicePackageCreateRequest;
import com.chamcham.backend.entity.PackageTier;
import com.chamcham.backend.dto.servicepackage.ServicePackageResponse;
import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.enums.PackagePricingType;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.ServicePackageMapper;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.ServicePackageRepository;
import com.chamcham.backend.util.PageResponse;
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
public class ServicePackageService {

    private final ServicePackageRepository servicePackageRepository;
    private final CreatorRepository creatorRepository;
    private final ServicePackageMapper servicePackageMapper;

    public ServicePackageService(
            ServicePackageRepository servicePackageRepository,
            CreatorRepository creatorRepository,
            ServicePackageMapper servicePackageMapper
    ) {
        this.servicePackageRepository = servicePackageRepository;
        this.creatorRepository = creatorRepository;
        this.servicePackageMapper = servicePackageMapper;
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

        ServicePackage servicePackage = ServicePackage.builder()
                .creator(creator)
                .name(packageName)
                .title(request.title())
                .description(request.description())
                .platform(request.platform())
                .category(request.category())
                .type(request.type())
                .pricingType(request.pricingType() == null ? PackagePricingType.PAID : request.pricingType())
                .barterDetails(request.barterDetails())
                .price(request.price())
                .currency(request.currency() == null || request.currency().isBlank() ? "PKR" : request.currency())
                .deliverables(request.deliverables())
                .deliveryDays(request.deliveryDays())
                .durationDays(request.durationDays())
                .revisions(request.revisions() == null ? 1 : request.revisions())
                .featured(request.isFeatured() != null && request.isFeatured())
                .mediaUrls(toArray(request.mediaUrls()))
                .tags(toArray(request.tags()))
                .active(request.isActive() == null || request.isActive())
                .coverImage(request.coverImage())
                .build();

        if (request.tiers() != null && !request.tiers().isEmpty()) {
            List<PackageTier> tiers = request.tiers().stream()
                    .map(tier -> PackageTier.builder()
                            .servicePackage(servicePackage)
                            .name(tier.name())
                            .price(tier.price())
                            .deliverables(tier.deliverables())
                            .deliveryDays(tier.deliveryDays())
                            .revisions(tier.revisions() == null ? 1 : tier.revisions())
                            .build())
                    .toList();
            servicePackage.setTiers(tiers);
        }

        return servicePackageMapper.toResponse(servicePackageRepository.save(servicePackage));
    }

    public void deletePackage(UUID packageId, UUID userId, UserRole role) {
        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));

        if (!role.isAdmin() && !servicePackage.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid request! Cannot delete other creator packages!");
        }

        servicePackageRepository.delete(servicePackage);
    }

    public ServicePackageResponse getPackage(UUID packageId) {
        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found!"));
        return servicePackageMapper.toResponse(servicePackage);
    }

    public PageResponse<ServicePackageResponse> getPackages(
            String category,
            String search,
            BigDecimal min,
            BigDecimal max,
            UUID creatorId,
            UUID creatorUserId,
            int page,
            int size,
            String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<ServicePackage> packages;

        if (creatorId != null || creatorUserId != null) {
            UUID resolvedCreatorId = creatorId != null ? creatorId : creatorUserId;
            Creator creator = creatorRepository.findById(resolvedCreatorId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
            packages = servicePackageRepository.findByCreator(creator, pageable);
        } else {
            packages = servicePackageRepository.findByActiveTrueAndCategoryContainingIgnoreCaseAndTitleContainingIgnoreCaseAndPriceBetween(
                    category == null ? "" : category,
                    search == null ? "" : search,
                    min == null ? BigDecimal.ZERO : min,
                    max == null ? new BigDecimal("999999999") : max,
                    pageable
            );
        }

        return PageResponse.from(packages.map(servicePackageMapper::toResponse));
    }

    private String[] toArray(List<String> values) {
        return values == null || values.isEmpty() ? null : values.toArray(String[]::new);
    }

    private void validatePricing(ServicePackageCreateRequest request) {
        PackagePricingType pricingType = request.pricingType() == null ? PackagePricingType.PAID : request.pricingType();

        if (pricingType == PackagePricingType.BARTER
                && (request.barterDetails() == null || request.barterDetails().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "barterDetails is required when pricingType is BARTER");
        }
    }
}

