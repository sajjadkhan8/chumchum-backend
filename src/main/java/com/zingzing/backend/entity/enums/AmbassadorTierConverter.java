package com.zingzing.backend.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = false)
public class AmbassadorTierConverter implements AttributeConverter<AmbassadorTier, String> {

    @Override
    public String convertToDatabaseColumn(AmbassadorTier attribute) {
        return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public AmbassadorTier convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String normalized = dbData.trim().toUpperCase(Locale.ROOT);
        if ("ELITE_CREATOR".equals(normalized) || "TOP_CREATOR".equals(normalized)) {
            return AmbassadorTier.ELITE_AMBASSADOR;
        }
        try {
            return AmbassadorTier.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid AmbassadorTier value in database: " + dbData, ex);
        }
    }
}
