package com.chamcham.backend.service;

import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.Package;
import com.chamcham.backend.entity.PackageAnalytics;
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

    public Map<String, Object> creatorInsights(UUID userId, UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        List<Package> packages = servicePackageRepository.findByCreatorId(userId);
        long totalViews = 0, totalClicks = 0, totalInquiries = 0, totalRepeatBrands = 0;
        double totalConversionRate = 0;
        int packageCount = 0;

        for (Package pkg : packages) {
            PackageAnalytics a = pkg.getAnalytics();
            if (a != null) {
                totalViews        += a.getViews();
                totalClicks       += a.getClicks();
                totalInquiries    += a.getInquiries();
                totalRepeatBrands += a.getRepeatBrands();
                totalConversionRate += a.getConversionRate().doubleValue();
                packageCount++;
            }
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

        return Map.of("totals", totals, "monthlyInquiryTrend", List.of(), "platformContribution", List.of(), "topPackages", List.of());
    }

    // ---- Creator Performance ----

    public Map<String, Object> creatorPerformance(UUID userId, UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        List<Package> packages = servicePackageRepository.findByCreatorId(userId);
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
}

