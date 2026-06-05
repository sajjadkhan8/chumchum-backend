package com.chamcham.backend.service;

import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.PackageAnalytics;
import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.Wallet;
import com.chamcham.backend.entity.enums.OrderStatus;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.OrderRepository;
import com.chamcham.backend.repository.PackageAnalyticsRepository;
import com.chamcham.backend.repository.ReviewRepository;
import com.chamcham.backend.repository.ServicePackageRepository;
import com.chamcham.backend.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final CreatorRepository creatorRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final WalletRepository walletRepository;
    private final PackageAnalyticsRepository packageAnalyticsRepository;
    private final ServicePackageRepository servicePackageRepository;

    public AnalyticsService(CreatorRepository creatorRepository, OrderRepository orderRepository,
                            ReviewRepository reviewRepository, WalletRepository walletRepository,
                            PackageAnalyticsRepository packageAnalyticsRepository,
                            ServicePackageRepository servicePackageRepository) {
        this.creatorRepository = creatorRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.walletRepository = walletRepository;
        this.packageAnalyticsRepository = packageAnalyticsRepository;
        this.servicePackageRepository = servicePackageRepository;
    }

    public record CreatorDashboard(
            long totalOrders, long activeOrders, long completedOrders,
            long totalEarnings, double avgRating, long totalReviews,
            long repeatBrands
    ) {}

    public record BrandDashboard(
            long totalOrders, long activeOrders, long completedOrders,
            long savedCreators
    ) {}

    @Transactional(readOnly = true)
    public CreatorDashboard creatorDashboard(UUID userId, UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        Creator creator = creatorRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));

        long total = orderRepository.countByCreatorIdAndStatusIn(userId,
                List.of(OrderStatus.values()));
        long active = orderRepository.countByCreatorIdAndStatusIn(userId,
                List.of(OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS, OrderStatus.DELIVERED,
                        OrderStatus.REVIEW, OrderStatus.REVISION));
        long completed = orderRepository.countByCreatorIdAndStatusIn(userId,
                List.of(OrderStatus.COMPLETED));
        long repeatBrands = orderRepository.countDistinctBrandsByCreatorAndCompleted(userId);

        Wallet wallet = walletRepository.findByCreatorId(userId).orElse(null);
        long totalEarnings = wallet != null ? wallet.getTotalEarned() : 0L;

        return new CreatorDashboard(total, active, completed, totalEarnings,
                creator.getRating().doubleValue(), creator.getTotalReviews(), repeatBrands);
    }

    @Transactional(readOnly = true)
    public BrandDashboard brandDashboard(UUID userId, UserRole role) {
        if (!role.isBrand()) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        long total = orderRepository.countByBrandIdAndStatusIn(userId, List.of(OrderStatus.values()));
        long active = orderRepository.countByBrandIdAndStatusIn(userId,
                List.of(OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS, OrderStatus.DELIVERED,
                        OrderStatus.REVIEW, OrderStatus.REVISION));
        long completed = orderRepository.countByBrandIdAndStatusIn(userId, List.of(OrderStatus.COMPLETED));

        return new BrandDashboard(total, active, completed, 0L);
    }

    // ---- Creator Insights ----

    @Transactional(readOnly = true)
    public Map<String, Object> creatorInsights(UUID userId, UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        List<ServicePackage> packages = servicePackageRepository.findByCreatorId(userId);
        long totalViews = 0, totalClicks = 0, totalInquiries = 0, totalRepeatBrands = 0;
        double totalConversionRate = 0;
        int packageCount = 0;
        Map<String, PlatformAccumulator> platformTotals = new HashMap<>();
        List<Map<String, Object>> topPackages = new ArrayList<>();

        for (ServicePackage pkg : packages) {
            PackageAnalytics a = pkg.getAnalytics();
            int views = a != null ? a.getViews() : 0;
            int clicks = a != null ? a.getClicks() : 0;
            int inquiries = a != null ? a.getInquiries() : 0;
            int repeatBrands = a != null ? a.getRepeatBrands() : 0;
            double conversionRate = a != null ? a.getConversionRate().doubleValue() : 0;
            double completionRate = a != null ? a.getCompletionRate().doubleValue() : 0;

            totalViews        += views;
            totalClicks       += clicks;
            totalInquiries    += inquiries;
            totalRepeatBrands += repeatBrands;
            totalConversionRate += conversionRate;
            packageCount++;

            String platform = pkg.getPlatform() != null ? pkg.getPlatform().name().toLowerCase() : "unknown";
            platformTotals.computeIfAbsent(platform, ignored -> new PlatformAccumulator())
                    .add(views, clicks, inquiries);

            Map<String, Object> topPackage = new LinkedHashMap<>();
            topPackage.put("packageId", pkg.getId());
            topPackage.put("title", pkg.getTitle());
            topPackage.put("platform", platform);
            topPackage.put("views", views);
            topPackage.put("clicks", clicks);
            topPackage.put("inquiries", inquiries);
            topPackage.put("conversionRate", conversionRate);
            topPackage.put("completionRate", completionRate);
            topPackage.put("repeatBrands", repeatBrands);
            topPackages.add(topPackage);
        }

        double avgConversionRate = packageCount > 0 ? totalConversionRate / packageCount : 0;

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("packageViews", totalViews);
        totals.put("packageViewsChange", 0.0);
        totals.put("inquiries", totalInquiries);
        totals.put("inquiriesChange", 0.0);
        totals.put("repeatBrands", totalRepeatBrands);
        totals.put("repeatBrandsChange", 0.0);
        totals.put("avgConversionRate", Math.round(avgConversionRate * 10.0) / 10.0);
        totals.put("avgConversionChange", 0.0);

        long finalTotalViews = totalViews;
        List<Map<String, Object>> platformContribution = platformTotals.entrySet().stream()
                .map(entry -> {
                    PlatformAccumulator value = entry.getValue();
                    Map<String, Object> platform = new LinkedHashMap<>();
                    platform.put("platform", entry.getKey());
                    platform.put("views", value.views);
                    platform.put("clicks", value.clicks);
                    platform.put("inquiries", value.inquiries);
                    platform.put("packageCount", value.packageCount);
                    platform.put("score", finalTotalViews > 0
                            ? Math.round((value.views * 1000.0 / finalTotalViews)) / 10.0
                            : 0.0);
                    return platform;
                })
                .sorted((a, b) -> Double.compare((double) b.get("score"), (double) a.get("score")))
                .toList();

        List<Map<String, Object>> sortedTopPackages = topPackages.stream()
                .sorted(Comparator.comparingDouble((Map<String, Object> item) -> (double) item.get("conversionRate")).reversed())
                .limit(5)
                .toList();

        return Map.of(
                "totals", totals,
                "monthlyInquiryTrend", List.of(),
                "platformContribution", platformContribution,
                "topPackages", sortedTopPackages
        );
    }

    // ---- Creator Performance ----

    @Transactional(readOnly = true)
    public Map<String, Object> creatorPerformance(UUID userId, UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        List<ServicePackage> packages = servicePackageRepository.findByCreatorId(userId);
        List<Map<String, Object>> rows = packages.stream().map(pkg -> {
            PackageAnalytics a = pkg.getAnalytics();
            int views = a != null ? a.getViews() : 0;
            int clicks = a != null ? a.getClicks() : 0;
            int inquiries = a != null ? a.getInquiries() : 0;
            double conversionRate = a != null ? a.getConversionRate().doubleValue() : 0;
            double completionRate = a != null ? a.getCompletionRate().doubleValue() : 0;
            int repeatBrands = a != null ? a.getRepeatBrands() : 0;
            double ctr = views > 0 ? Math.round((clicks * 1000.0 / views)) / 10.0 : 0;
            double inquiryToClick = clicks > 0 ? Math.round((inquiries * 1000.0 / clicks)) / 10.0 : 0;
            int efficiencyScore = (int) Math.min(100, (conversionRate * 4 + completionRate * 0.5 + ctr * 0.3));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("packageId", pkg.getId());
            row.put("title", pkg.getTitle());
            row.put("views", views);
            row.put("clicks", clicks);
            row.put("inquiries", inquiries);
            row.put("conversionRate", conversionRate);
            row.put("completionRate", completionRate);
            row.put("repeatBrands", repeatBrands);
            row.put("ctr", ctr);
            row.put("inquiryToClickRate", inquiryToClick);
            row.put("efficiencyScore", efficiencyScore);
            return row;
        }).toList();

        return Map.of("packages", rows);
    }

    // ---- Brand Campaigns ----

    @Transactional(readOnly = true)
    public Map<String, Object> brandCampaigns(UUID userId, UserRole role) {
        if (!role.isBrand()) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        long totalOrders = orderRepository.countByBrandIdAndStatusIn(userId, List.of(OrderStatus.values()));
        long completedOrders = orderRepository.countByBrandIdAndStatusIn(userId, List.of(OrderStatus.COMPLETED));
        long activeCreators = orderRepository.countByBrandIdAndStatusIn(userId,
                List.of(OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS, OrderStatus.DELIVERED,
                        OrderStatus.REVIEW, OrderStatus.REVISION));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalReach", 0L);
        data.put("avgEngagementRate", 0.0);
        data.put("creatorsActive", activeCreators);
        data.put("monthlySpend", 0L);
        data.put("totalOrders", totalOrders);
        data.put("completedOrders", completedOrders);
        data.put("topCities", List.of());
        data.put("dealMix", Map.of("paid", 0, "hybrid", 0, "barter", 0));

        return data;
    }

    private static class PlatformAccumulator {
        private long views = 0;
        private long clicks = 0;
        private long inquiries = 0;
        private long packageCount = 0;

        private void add(int packageViews, int packageClicks, int packageInquiries) {
            views += packageViews;
            clicks += packageClicks;
            inquiries += packageInquiries;
            packageCount++;
        }
    }
}
