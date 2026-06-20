package com.zingzing.backend.service;

import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.PackageAnalytics;
import com.zingzing.backend.entity.PackageAnalyticsEvent;
import com.zingzing.backend.entity.ServicePackage;
import com.zingzing.backend.entity.User;
import com.zingzing.backend.repository.PackageAnalyticsEventRepository;
import com.zingzing.backend.repository.PackageAnalyticsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PackageAnalyticsTrackingService {

    private final PackageAnalyticsRepository packageAnalyticsRepository;
    private final PackageAnalyticsEventRepository packageAnalyticsEventRepository;

    public PackageAnalyticsTrackingService(PackageAnalyticsRepository packageAnalyticsRepository,
                                           PackageAnalyticsEventRepository packageAnalyticsEventRepository) {
        this.packageAnalyticsRepository = packageAnalyticsRepository;
        this.packageAnalyticsEventRepository = packageAnalyticsEventRepository;
    }

    @Transactional
    public void track(ServicePackage pkg, String eventType, User actor, Brand brand, String source, String metadata) {
        if (pkg == null || eventType == null || eventType.isBlank()) return;
        String normalized = eventType.trim().toUpperCase();
        if (!normalized.equals("VIEW") && !normalized.equals("CLICK") && !normalized.equals("INQUIRY")) {
            normalized = "VIEW";
        }

        packageAnalyticsEventRepository.save(PackageAnalyticsEvent.builder()
                .servicePackage(pkg)
                .creator(pkg.getCreator())
                .brand(brand)
                .actor(actor)
                .eventType(normalized)
                .source(source == null || source.isBlank() ? "api" : source.trim())
                .metadata(metadata == null || metadata.isBlank() ? "{}" : metadata)
                .build());

        PackageAnalytics analytics = packageAnalyticsRepository.findByServicePackageId(pkg.getId())
                .orElseGet(() -> PackageAnalytics.builder().servicePackage(pkg).build());
        switch (normalized) {
            case "VIEW" -> analytics.setViews(analytics.getViews() + 1);
            case "CLICK" -> analytics.setClicks(analytics.getClicks() + 1);
            case "INQUIRY" -> analytics.setInquiries(analytics.getInquiries() + 1);
            default -> { }
        }
        analytics.setConversionRate(conversionRate(analytics.getInquiries(), analytics.getViews()));
        packageAnalyticsRepository.save(analytics);
    }

    private BigDecimal conversionRate(int inquiries, int views) {
        if (views <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(inquiries)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(views), 2, RoundingMode.HALF_UP);
    }
}
