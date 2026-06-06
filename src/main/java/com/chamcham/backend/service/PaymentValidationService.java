package com.chamcham.backend.service;

import com.chamcham.backend.entity.enums.PayoutMethodType;
import com.chamcham.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PaymentValidationService {

    public void validateCreatorPayoutDetails(PayoutMethodType type, String accountDetails) {
        String normalized = normalize(accountDetails);
        if (normalized.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Account details are required");
        }

        if (type == PayoutMethodType.BANK_TRANSFER) {
            if (!normalized.matches("^PK\\d{2}[A-Z0-9]{20,30}$")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Pakistan IBAN format");
            }
            return;
        }

        if (!normalized.replace("-", "").matches("^\\+?\\d{10,15}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid wallet number format");
        }
    }

    public void validateCnicLast4(String cnicLast4) {
        if (cnicLast4 == null || cnicLast4.isBlank()) return;
        if (!cnicLast4.matches("^\\d{4}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CNIC last 4 digits must contain exactly 4 numbers");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}

