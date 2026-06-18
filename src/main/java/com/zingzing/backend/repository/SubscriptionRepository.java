package com.zingzing.backend.repository;

import com.zingzing.backend.entity.Subscription;
import com.zingzing.backend.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByBrandIdOrderByCreatedAtDesc(UUID brandId);

    Optional<Subscription> findByBrandIdAndServicePackageIdAndStatus(
            UUID brandId, UUID packageId, SubscriptionStatus status);

    List<Subscription> findByStatusAndNextRenewalAtBefore(SubscriptionStatus status, Instant cutoff);
}
