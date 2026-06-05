package com.chamcham.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MetadataController {

    @GetMapping({"/creators/metadata", "/creators/filters", "/metadata/creators"})
    public ResponseEntity<Map<String, Object>> creatorFilterMetadata() {
        Map<String, Object> metadata = Map.of(
                "categories", List.of(
                        "Food",
                        "Fashion",
                        "Beauty",
                        "Tech",
                        "Gaming",
                        "Travel",
                        "Fitness",
                        "Health",
                        "Lifestyle",
                        "Comedy",
                        "Entertainment",
                        "Education",
                        "Parenting",
                        "Automotive",
                        "Cooking",
                        "Vlogging",
                        "Reviews"
                ),
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
                        range(0, 625_000, "Under PKR 625,000"),
                        range(625_000, 1_250_000, "PKR 625,000 - 1,250,000"),
                        range(1_250_000, 2_500_000, "PKR 1,250,000 - 2,500,000"),
                        range(2_500_000, 6_250_000, "PKR 2,500,000 - 6,250,000"),
                        range(6_250_000, Integer.MAX_VALUE, "PKR 6,250,000+")
                )
        );

        return ResponseEntity.ok(Map.of("success", true, "data", metadata));
    }

    private Map<String, Object> option(String value, String label) {
        return Map.of("value", value, "label", label);
    }

    private Map<String, Object> range(int min, int max, String label) {
        return Map.of("min", min, "max", max, "label", label);
    }
}
