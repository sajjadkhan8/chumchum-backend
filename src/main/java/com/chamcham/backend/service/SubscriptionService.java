package com.chamcham.backend.service;

import com.chamcham.backend.dto.subscription.SubscriptionResponse;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.Subscription;
import com.chamcham.backend.entity.enums.PackageType;
import com.chamcham.backend.entity.enums.SubscriptionStatus;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.ServicePackageRepository;
import com.chamcham.backend.repository.SubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final BrandRepository brandRepository;
    private final ServicePackageRepository packageRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               BrandRepository brandRepository,
                               ServicePackageRepository packageRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.brandRepository = brandRepository;
        this.packageRepository = packageRepository;
    }

    @Transactional
    public SubscriptionResponse subscribe(UUID brandId, UserRole role, UUID packageId) {
        if (!role.isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can subscribe to packages");
        }
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand not found"));
        ServicePackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));

        if (pkg.getType() != PackageType.SUBSCRIPTION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This package does not support recurring subscriptions");
        }
        if (pkg.getSubscriptionInterval() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Package is missing subscription interval configuration");
        }

        subscriptionRepository.findByBrandIdAndServicePackageIdAndStatus(brandId, packageId, SubscriptionStatus.ACTIVE)
                .ifPresent(s -> { throw new ApiException(HttpStatus.CONFLICT, "You already have an active subscription to this package"); });

        Instant firstRenewal = computeNextRenewal(Instant.now(), pkg);

        Subscription sub = Subscription.builder()
                .brand(brand)
                .servicePackage(pkg)
                .interval(pkg.getSubscriptionInterval())
                .duration(pkg.getSubscriptionDuration() != null ? pkg.getSubscriptionDuration() : 1)
                .nextRenewalAt(firstRenewal)
                .build();

        return toResponse(subscriptionRepository.save(sub));
    }

    @Transactional
    public SubscriptionResponse cancel(UUID brandId, UserRole role, UUID subscriptionId) {
        if (!role.isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can cancel subscriptions");
        }
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));
        if (!sub.getBrand().getId().equals(brandId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your subscription");
        }
        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Subscription is not active");
        }
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setCancelledAt(Instant.now());
        return toResponse(subscriptionRepository.save(sub));
    }

    public List<SubscriptionResponse> getMySubscriptions(UUID brandId, UserRole role) {
        if (!role.isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can view subscriptions");
        }
        return subscriptionRepository.findByBrandIdOrderByCreatedAtDesc(brandId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void processRenewals() {
        List<Subscription> due = subscriptionRepository.findByStatusAndNextRenewalAtBefore(
                SubscriptionStatus.ACTIVE, Instant.now());
        for (Subscription sub : due) {
            int nextCycle = sub.getCyclesCompleted() + 1;
            if (sub.getDuration() > 0 && nextCycle >= sub.getDuration()) {
                sub.setStatus(SubscriptionStatus.EXPIRED);
            } else {
                sub.setCyclesCompleted(nextCycle);
                sub.setNextRenewalAt(computeNextRenewal(sub.getNextRenewalAt(), sub.getServicePackage()));
            }
            subscriptionRepository.save(sub);
        }
    }

    private Instant computeNextRenewal(Instant from, ServicePackage pkg) {
        if (pkg.getSubscriptionInterval() == null) return from.plus(30, ChronoUnit.DAYS);
        return switch (pkg.getSubscriptionInterval()) {
            case WEEKLY -> from.plus(7, ChronoUnit.DAYS);
            case MONTHLY -> from.plus(30, ChronoUnit.DAYS);
            case QUARTERLY -> from.plus(90, ChronoUnit.DAYS);
        };
    }

    private SubscriptionResponse toResponse(Subscription sub) {
        return new SubscriptionResponse(
                sub.getId(),
                sub.getBrand().getId(),
                sub.getServicePackage().getId(),
                sub.getServicePackage().getTitle(),
                sub.getStatus(),
                sub.getInterval(),
                sub.getDuration(),
                sub.getCyclesCompleted(),
                sub.getNextRenewalAt(),
                sub.getCancelledAt(),
                sub.getCreatedAt()
        );
    }
}
