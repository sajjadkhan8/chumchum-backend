package com.zingzing.backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum PackageCategory {
    FOOD,
    FASHION,
    BEAUTY,
    TECH,
    FITNESS,
    HEALTH,
    TRAVEL,
    LIFESTYLE,
    GAMING,
    EDUCATION,
    ENTERTAINMENT,
    BUSINESS_FINANCE,
    HOME_DECOR,
    PARENTING_FAMILY,
    SPORTS,
    AUTOMOTIVE,
    RELIGIOUS_SPIRITUAL,
    GENERAL,
    QUICK_DEAL;

    private static final Map<String, PackageCategory> LEGACY_ALIASES = Map.ofEntries(
            Map.entry("FASHION_BEAUTY", FASHION),
            Map.entry("FOOD_BEVERAGE", FOOD),
            Map.entry("TECHNOLOGY_GADGETS", TECH),
            Map.entry("FITNESS_HEALTH", FITNESS),
            Map.entry("TRAVEL_LIFESTYLE", TRAVEL),
            Map.entry("ENTERTAINMENT_COMEDY", ENTERTAINMENT),
            Map.entry("EDUCATION_CAREER", EDUCATION),
            Map.entry("FOOD & BEVERAGE", FOOD),
            Map.entry("TECHNOLOGY & GADGETS", TECH),
            Map.entry("FASHION & BEAUTY", FASHION),
            Map.entry("FITNESS & HEALTH", FITNESS),
            Map.entry("TRAVEL & LIFESTYLE", TRAVEL),
            Map.entry("ENTERTAINMENT & COMEDY", ENTERTAINMENT),
            Map.entry("EDUCATION & CAREER", EDUCATION),
            Map.entry("BUSINESS & FINANCE", BUSINESS_FINANCE),
            Map.entry("HOME & DECOR", HOME_DECOR),
            Map.entry("PARENTING & FAMILY", PARENTING_FAMILY),
            Map.entry("RELIGIOUS & SPIRITUAL", RELIGIOUS_SPIRITUAL),
            Map.entry("TECHNOLOGY", TECH),
            Map.entry("COMEDY", ENTERTAINMENT),
            Map.entry("COOKING", FOOD),
            Map.entry("VLOGGING", LIFESTYLE),
            Map.entry("REVIEWS", TECH),
            Map.entry("PARENTING", PARENTING_FAMILY)
    );

    public static final List<PackageCategory> PUBLIC_CATEGORIES = List.of(
            FOOD,
            FASHION,
            BEAUTY,
            TECH,
            FITNESS,
            HEALTH,
            TRAVEL,
            LIFESTYLE,
            GAMING,
            EDUCATION,
            ENTERTAINMENT,
            BUSINESS_FINANCE,
            HOME_DECOR,
            PARENTING_FAMILY,
            SPORTS,
            AUTOMOTIVE,
            RELIGIOUS_SPIRITUAL,
            GENERAL
    );

    @JsonCreator
    public static PackageCategory from(String rawValue) {
        return normalize(rawValue).orElseThrow(() ->
                new IllegalArgumentException("Unsupported package category: " + rawValue));
    }

    public static Optional<PackageCategory> normalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }

        String normalized = rawValue.trim()
                .replace('-', '_')
                .replace('&', ' ')
                .replaceAll("\\s+", "_")
                .toUpperCase(Locale.ROOT);

        if (LEGACY_ALIASES.containsKey(normalized)) {
            return Optional.of(LEGACY_ALIASES.get(normalized));
        }

        return Arrays.stream(values())
                .filter(category -> category.name().equals(normalized))
                .findFirst();
    }

    public static List<String> normalizeCreatorCategories(List<String> rawCategories) {
        if (rawCategories == null) {
            return List.of();
        }

        return rawCategories.stream()
                .map(PackageCategory::normalize)
                .flatMap(Optional::stream)
                .filter(PUBLIC_CATEGORIES::contains)
                .map(PackageCategory::name)
                .distinct()
                .collect(Collectors.toList());
    }

    public String label() {
        return switch (this) {
            case FOOD -> "Food";
            case FASHION -> "Fashion";
            case BEAUTY -> "Beauty";
            case TECH -> "Tech";
            case FITNESS -> "Fitness";
            case HEALTH -> "Health";
            case TRAVEL -> "Travel";
            case LIFESTYLE -> "Lifestyle";
            case GAMING -> "Gaming";
            case EDUCATION -> "Education";
            case ENTERTAINMENT -> "Entertainment";
            case BUSINESS_FINANCE -> "Business & Finance";
            case HOME_DECOR -> "Home & Decor";
            case PARENTING_FAMILY -> "Parenting & Family";
            case SPORTS -> "Sports";
            case AUTOMOTIVE -> "Automotive";
            case RELIGIOUS_SPIRITUAL -> "Religious & Spiritual";
            case GENERAL -> "General";
            case QUICK_DEAL -> "Quick Deal";
        };
    }
}
