package com.zingzing.backend.repository;

import com.zingzing.backend.entity.PackageAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PackageAnalyticsRepository extends JpaRepository<PackageAnalytics, UUID> {
    Optional<PackageAnalytics> findByServicePackageId(UUID packageId);
}

