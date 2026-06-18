package com.zingzing.backend.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = false)
public class OfferStatusConverter implements AttributeConverter<OfferStatus, String> {

    @Override
    public String convertToDatabaseColumn(OfferStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public OfferStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OfferStatus.valueOf(dbData.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid OfferStatus value in database: " + dbData, ex);
        }
    }
}

