package com.zingzing.backend.repository;

import com.zingzing.backend.entity.PackageAnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PackageAnalyticsEventRepository extends JpaRepository<PackageAnalyticsEvent, UUID> {
}
