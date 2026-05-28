package com.chamcham.backend.repository;

import com.chamcham.backend.entity.PackageAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PackageAnalyticsRepository extends JpaRepository<PackageAnalytics, Object> {
    Optional<PackageAnalytics> findByServicePackageId(java.util.UUID packageId);
}

