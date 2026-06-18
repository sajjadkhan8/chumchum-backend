package com.zingzing.backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum CreatorPayoutSchedule {
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    MANUAL;

    @JsonCreator
    public static CreatorPayoutSchedule fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return CreatorPayoutSchedule.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}

