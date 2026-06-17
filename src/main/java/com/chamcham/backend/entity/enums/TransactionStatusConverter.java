package com.chamcham.backend.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = false)
public class TransactionStatusConverter implements AttributeConverter<TransactionStatus, String> {

    @Override
    public String convertToDatabaseColumn(TransactionStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public TransactionStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return TransactionStatus.valueOf(dbData.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid TransactionStatus value in database: " + dbData, ex);
        }
    }
}
