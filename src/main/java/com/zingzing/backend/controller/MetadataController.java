package com.zingzing.backend.controller;

import com.zingzing.backend.entity.enums.PackageCategory;
import com.zingzing.backend.repository.CreatorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MetadataController {

    private final CreatorRepository creatorRepository;

    public MetadataController(CreatorRepository creatorRepository) {
        this.creatorRepository = creatorRepository;
    }

    @GetMapping({"/creators/metadata", "/creators/filters", "/metadata/creators"})
    public ResponseEntity<Map<String, Object>> creatorFilterMetadata() {
        Map<String, Object> metadata = Map.of(
                "categories", categoryValues(),
                "categoryOptions", categoryOptions(),
                "cities", List.of("Karachi", "Lahore", "Islamabad", "Rawalpindi", "Faisalabad", "Multan", "Peshawar"),
                "platforms", List.of("instagram", "tiktok", "youtube", "facebook"),
                "dealTypes", List.of(
                        option("paid", "Paid"),
                        option("barter", "Barter"),
                        option("hybrid", "Hybrid")
                ),
                "barterTypes", List.of(
                        option("food", "Food & Dining"),
                        option("hotel", "Hotels & Stays"),
                        option("salon", "Salon & Spa"),
                        option("events", "Events & Tickets"),
                        option("products", "Products")
                ),
                "followerRanges", List.of(
                        range(0, 10_000, "Nano (0-10K)"),
                        range(10_000, 50_000, "Micro (10K-50K)"),
                        range(50_000, 500_000, "Mid-tier (50K-500K)"),
                        range(500_000, 1_000_000, "Macro (500K-1M)"),
                        range(1_000_000, Integer.MAX_VALUE, "Mega (1M+)")
                ),
                "priceRanges", List.of(
                        range(0, 25_000, "Under PKR 25,000"),
                        range(25_000, 50_000, "PKR 25,000 - 50,000"),
                        range(50_000, 100_000, "PKR 50,000 - 100,000"),
                        range(100_000, 200_000, "PKR 100,000 - 200,000"),
                        range(200_000, Integer.MAX_VALUE, "PKR 200,000+")
                )
        );

        return ResponseEntity.ok(Map.of("success", true, "data", metadata));
    }

    @GetMapping("/metadata/search-filters")
    public ResponseEntity<Map<String, Object>> searchFilters() {
        List<String> languages  = creatorRepository.findDistinctLanguages();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "categories", categoryValues(),
                        "categoryOptions", categoryOptions(),
                        "languages", languages
                )
        ));
    }

    private List<String> categoryValues() {
        return PackageCategory.PUBLIC_CATEGORIES.stream()
                .map(PackageCategory::name)
                .toList();
    }

    private List<Map<String, Object>> categoryOptions() {
        return PackageCategory.PUBLIC_CATEGORIES.stream()
                .map(category -> option(category.name(), category.label()))
                .toList();
    }

    private Map<String, Object> option(String value, String label) {
        return Map.of("value", value, "label", label);
    }

    private Map<String, Object> range(int min, int max, String label) {
        return Map.of("min", min, "max", max, "label", label);
    }
}
