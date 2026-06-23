package com.zingzing.backend.service;

import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.Order;
import com.zingzing.backend.entity.PackageAnalytics;
import com.zingzing.backend.entity.ServicePackage;
import com.zingzing.backend.entity.Wallet;
import com.zingzing.backend.entity.enums.OrderStatus;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.OrderRepository;
import com.zingzing.backend.repository.PackageAnalyticsRepository;
import com.zingzing.backend.repository.ReviewRepository;
import com.zingzing.backend.repository.SavedCreatorRepository;
import com.zingzing.backend.repository.ServicePackageRepository;
import com.zingzing.backend.repository.WalletRepository;
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
    private final SavedCreatorRepository savedCreatorRepository;

    public AnalyticsService(CreatorRepository creatorRepository, OrderRepository orderRepository,
                            ReviewRepository reviewRepository, WalletRepository walletRepository,
                            PackageAnalyticsRepository packageAnalyticsRepository,
                            ServicePackageRepository servicePackageRepository,
                            SavedCreatorRepository savedCreatorRepository) {
        this.creatorRepository = creatorRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.walletRepository = walletRepository;
        this.packageAnalyticsRepository = packageAnalyticsRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.savedCreatorRepository = savedCreatorRepository;
    }

    public record CreatorDashboard(
            long totalOrders, long activeOrders, long completedOrders,
            long totalEarnings, double avgRating, long totalReviews,
            long repeatBrands
    ) {}

    public record BrandDashboard(
            long totalOrders, long activeOrders, long completedOrders,
            long savedCreators, long totalSpent, long creatorsWorkedWith,
            double avgRating
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
        long totalSpent = orderRepository.findByBrandIdOrderByCreatedAtDesc(userId).stream()
                .filter(order -> order.getAmount() != null)
                .mapToLong(Order::getAmount)
                .sum();
        long creatorsWorkedWith = orderRepository.countDistinctCreatorsByBrand(userId);
        long savedCreators = savedCreatorRepository.countByBrandId(userId);
        double avgRating = Math.round(reviewRepository.averageRatingByBrand(userId) * 10.0) / 10.0;

        return new BrandDashboard(total, active, completed, savedCreators, totalSpent, creatorsWorkedWith, avgRating);
    }

    // ---- Creator Insights ----

    @Transactional(readOnly = true)
    public Map<String, Object> creatorInsights(UUID userId, UserRole role, String period) {
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

        // Orders are the only timestamped creator inquiry signal (package analytics are cumulative
        // with no time-series), so we derive the trend and period-over-period deltas from them.
        int windowMonths = monthsForPeriod(period);
        List<Order> creatorOrders = orderRepository.findByCreatorIdOrderByCreatedAtDesc(userId);

        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Karachi");
        java.time.YearMonth currentMonth = java.time.YearMonth.now(zone);

        // Bucket order counts and distinct brands by month.
        Map<java.time.YearMonth, Long> ordersByMonth = new HashMap<>();
        Map<java.time.YearMonth, java.util.Set<UUID>> brandsByMonth = new HashMap<>();
        for (Order order : creatorOrders) {
            if (order.getCreatedAt() == null) continue;
            java.time.YearMonth ym = java.time.YearMonth.from(order.getCreatedAt().atZone(zone));
            ordersByMonth.merge(ym, 1L, Long::sum);
            brandsByMonth.computeIfAbsent(ym, ignored -> new java.util.HashSet<>()).add(order.getBrand().getId());
        }

        List<Map<String, Object>> monthlyInquiryTrend = new ArrayList<>();
        long currentWindowInquiries = 0;
        java.util.Set<UUID> currentWindowBrands = new java.util.HashSet<>();
        for (int i = windowMonths - 1; i >= 0; i--) {
            java.time.YearMonth ym = currentMonth.minusMonths(i);
            long value = ordersByMonth.getOrDefault(ym, 0L);
            currentWindowInquiries += value;
            currentWindowBrands.addAll(brandsByMonth.getOrDefault(ym, java.util.Set.of()));
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", ym.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH));
            point.put("value", value);
            monthlyInquiryTrend.add(point);
        }

        // Immediately-preceding equal-length window for period-over-period deltas.
        long previousWindowInquiries = 0;
        java.util.Set<UUID> previousWindowBrands = new java.util.HashSet<>();
        for (int i = windowMonths; i < windowMonths * 2; i++) {
            java.time.YearMonth ym = currentMonth.minusMonths(i);
            previousWindowInquiries += ordersByMonth.getOrDefault(ym, 0L);
            previousWindowBrands.addAll(brandsByMonth.getOrDefault(ym, java.util.Set.of()));
        }

        double inquiriesChange = percentChange(previousWindowInquiries, currentWindowInquiries);
        double repeatBrandsChange = percentChange(previousWindowBrands.size(), currentWindowBrands.size());

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("packageViews", totalViews);
        // No time-series backing for package views (cumulative analytics only) — left at 0.0.
        totals.put("packageViewsChange", 0.0);
        totals.put("inquiries", totalInquiries);
        totals.put("inquiriesChange", inquiriesChange);
        totals.put("repeatBrands", totalRepeatBrands);
        totals.put("repeatBrandsChange", repeatBrandsChange);
        totals.put("avgConversionRate", Math.round(avgConversionRate * 10.0) / 10.0);
        // No time-series backing for avg conversion (cumulative analytics only) — left at 0.0.
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
                "monthlyInquiryTrend", monthlyInquiryTrend,
                "platformContribution", platformContribution,
                "topPackages", sortedTopPackages
        );
    }

    /** Maps the period string to the number of trailing monthly buckets to emit. */
    private int monthsForPeriod(String period) {
        if (period == null) return 6;
        return switch (period) {
            case "30d" -> 1;
            case "90d" -> 3;
            case "1y" -> 12;
            default -> 6; // "6m" and anything unrecognized
        };
    }

    /** Percent change from previous to current, rounded 1 decimal; 0.0 when previous is 0. */
    private double percentChange(long previous, long current) {
        if (previous == 0) return 0.0;
        return Math.round(((current - previous) * 1000.0 / previous)) / 10.0;
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
    public Map<String, Object> brandCampaigns(UUID userId, UserRole role, String period) {
        if (!role.isBrand()) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        List<Order> orders = orderRepository.findByBrandIdOrderByCreatedAtDesc(userId);
        List<OrderStatus> activeStatuses = List.of(OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS, OrderStatus.DELIVERED,
                OrderStatus.REVIEW, OrderStatus.REVISION);
        long totalOrders = orders.size();
        long completedOrders = orders.stream().filter(order -> order.getStatus() == OrderStatus.COMPLETED).count();
        long activeCreators = orders.stream()
                .filter(order -> activeStatuses.contains(order.getStatus()))
                .map(order -> order.getCreator().getId())
                .distinct()
                .count();
        long monthlySpend = orders.stream()
                .filter(order -> order.getAmount() != null)
                .mapToLong(Order::getAmount)
                .sum();

        Map<String, Long> cityCounts = new HashMap<>();
        Map<String, Integer> dealMix = new LinkedHashMap<>();
        dealMix.put("paid", 0);
        dealMix.put("hybrid", 0);
        dealMix.put("barter", 0);

        orders.forEach(order -> {
            String dealType = order.getDealType() != null ? order.getDealType().name().toLowerCase() : "paid";
            dealMix.computeIfPresent(dealType, (key, value) -> value + 1);

            String city = order.getCreator().getCity() != null && !order.getCreator().getCity().isBlank()
                    ? order.getCreator().getCity().trim().toLowerCase()
                    : "unknown";
            cityCounts.merge(city, 1L, Long::sum);
        });

        List<Map<String, Object>> topCities = cityCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(entry -> {
                    Map<String, Object> city = new LinkedHashMap<>();
                    city.put("city", entry.getKey());
                    city.put("orders", entry.getValue());
                    city.put("share", totalOrders > 0
                            ? Math.round((entry.getValue() * 1000.0 / totalOrders)) / 10.0
                            : 0.0);
                    return city;
                })
                .toList();

        List<com.zingzing.backend.entity.Creator> completedCreators = orders.stream()
                .filter(order -> order.getStatus() == com.zingzing.backend.entity.enums.OrderStatus.COMPLETED)
                .map(com.zingzing.backend.entity.Order::getCreator)
                .distinct()
                .toList();
        long totalReach = completedCreators.stream()
                .mapToLong(c -> c.getFollowers())
                .sum();
        double avgEngagementRate = completedCreators.stream()
                .filter(c -> c.getEngagementRate() != null)
                .mapToDouble(c -> c.getEngagementRate().doubleValue())
                .average()
                .orElse(0.0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalReach", totalReach);
        data.put("avgEngagementRate", Math.round(avgEngagementRate * 10.0) / 10.0);
        data.put("creatorsActive", activeCreators);
        data.put("monthlySpend", monthlySpend);
        data.put("totalOrders", totalOrders);
        data.put("completedOrders", completedOrders);
        data.put("topCities", topCities);
        data.put("dealMix", dealMix);

        return data;
    }

    // ---- Brand Extended ----

    @Transactional(readOnly = true)
    public Map<String, Object> brandExtended(UUID userId, UserRole role, String period) {
        if (!role.isBrand()) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        List<Order> orders = orderRepository.findByBrandIdOrderByCreatedAtDesc(userId);

        // topCreatorsBySpend: group orders by creator
        Map<UUID, CreatorSpendAccumulator> creatorSpend = new LinkedHashMap<>();
        orders.forEach(order -> {
            Creator creator = order.getCreator();
            creatorSpend.computeIfAbsent(creator.getId(), ignored -> new CreatorSpendAccumulator(creator))
                    .add(order.getAmount(), order.getStatus() == OrderStatus.COMPLETED);
        });

        List<Map<String, Object>> topCreatorsBySpend = creatorSpend.values().stream()
                .sorted(Comparator.comparingLong((CreatorSpendAccumulator acc) -> acc.totalSpend).reversed())
                .limit(10)
                .map(acc -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("creatorId", acc.creator.getId());
                    row.put("creatorName", acc.creator.getName());
                    row.put("creatorAvatar", acc.creator.getAvatarUrl());
                    row.put("totalSpend", acc.totalSpend);
                    row.put("orderCount", acc.orderCount);
                    row.put("completedOrders", acc.completedOrders);
                    row.put("avgRating", acc.creator.getRating() != null ? acc.creator.getRating().doubleValue() : null);
                    return row;
                })
                .toList();

        // campaignCompletionRates: group by servicePackage (orders accepted from campaigns
        // create a ServicePackage, so grouping by package is the correct proxy for campaigns)
        Map<UUID, CampaignAccumulator> campaigns = new LinkedHashMap<>();
        orders.forEach(order -> {
            ServicePackage pkg = order.getServicePackage();
            campaigns.computeIfAbsent(pkg.getId(), ignored -> new CampaignAccumulator(pkg))
                    .add(order.getStatus() == OrderStatus.COMPLETED);
        });

        List<Map<String, Object>> campaignCompletionRates = campaigns.values().stream()
                .sorted(Comparator.comparingLong((CampaignAccumulator acc) -> acc.totalOrders).reversed())
                .limit(10)
                .map(acc -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("campaignId", acc.servicePackage.getId());
                    row.put("campaignTitle", acc.servicePackage.getTitle());
                    row.put("totalOrders", acc.totalOrders);
                    row.put("completedOrders", acc.completedOrders);
                    row.put("completionRate", acc.totalOrders > 0
                            ? Math.round((acc.completedOrders * 1000.0 / acc.totalOrders)) / 10.0
                            : 0.0);
                    return row;
                })
                .toList();

        // dealTypeROI: per deal type — count, total spend, avg engagement of those orders' creators
        Map<String, Object> dealTypeROI = new LinkedHashMap<>();
        dealTypeROI.put("paid", dealTypeRoiBucket(orders, com.zingzing.backend.entity.enums.DealType.PAID));
        dealTypeROI.put("barter", dealTypeRoiBucket(orders, com.zingzing.backend.entity.enums.DealType.BARTER));
        dealTypeROI.put("hybrid", dealTypeRoiBucket(orders, com.zingzing.backend.entity.enums.DealType.HYBRID));

        // repeatCreatorRate: percent of distinct creators ordered from more than once
        long distinctCreators = creatorSpend.size();
        long repeatCreators = creatorSpend.values().stream()
                .filter(acc -> acc.orderCount > 1)
                .count();
        double repeatCreatorRate = distinctCreators > 0
                ? Math.round((repeatCreators * 1000.0 / distinctCreators)) / 10.0
                : 0.0;

        // onTimeDeliveryPct: among completed orders, percent delivered on time.
        // updatedAt is used as the completion timestamp proxy (Order extends BaseEntity).
        List<Order> completed = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .toList();
        long onTime = completed.stream()
                .filter(order -> order.getDeadlineDate() == null
                        || (order.getUpdatedAt() != null
                            && !order.getUpdatedAt().isAfter(order.getDeadlineDate().toInstant())))
                .count();
        double onTimeDeliveryPct = !completed.isEmpty()
                ? Math.round((onTime * 1000.0 / completed.size())) / 10.0
                : 0.0;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("onTimeDeliveryPct", onTimeDeliveryPct);
        data.put("repeatCreatorRate", repeatCreatorRate);
        data.put("topCreatorsBySpend", topCreatorsBySpend);
        data.put("campaignCompletionRates", campaignCompletionRates);
        data.put("dealTypeROI", dealTypeROI);

        return data;
    }

    private Map<String, Object> dealTypeRoiBucket(List<Order> orders, com.zingzing.backend.entity.enums.DealType dealType) {
        List<Order> bucket = orders.stream()
                .filter(order -> order.getDealType() == dealType)
                .toList();
        long count = bucket.size();
        long totalSpend = bucket.stream()
                .filter(order -> order.getAmount() != null)
                .mapToLong(Order::getAmount)
                .sum();
        double avgEngagement = bucket.stream()
                .map(order -> order.getCreator().getEngagementRate())
                .filter(rate -> rate != null)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        Map<String, Object> roi = new LinkedHashMap<>();
        roi.put("count", count);
        roi.put("totalSpend", totalSpend);
        roi.put("avgEngagement", Math.round(avgEngagement * 10.0) / 10.0);
        return roi;
    }

    private static class CreatorSpendAccumulator {
        private final Creator creator;
        private long totalSpend = 0;
        private long orderCount = 0;
        private long completedOrders = 0;

        private CreatorSpendAccumulator(Creator creator) {
            this.creator = creator;
        }

        private void add(Integer amount, boolean completed) {
            totalSpend += amount != null ? amount : 0;
            orderCount++;
            if (completed) completedOrders++;
        }
    }

    private static class CampaignAccumulator {
        private final ServicePackage servicePackage;
        private long totalOrders = 0;
        private long completedOrders = 0;

        private CampaignAccumulator(ServicePackage servicePackage) {
            this.servicePackage = servicePackage;
        }

        private void add(boolean completed) {
            totalOrders++;
            if (completed) completedOrders++;
        }
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
