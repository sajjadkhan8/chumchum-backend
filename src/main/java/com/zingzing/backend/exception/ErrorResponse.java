package com.zingzing.backend.exception;

import java.util.List;

public record ErrorResponse(
        boolean success,
        ErrorBody error
) {

    public static ErrorResponse of(String code, String message, List<ErrorDetail> details) {
        return new ErrorResponse(false, new ErrorBody(code, message, details));
    }

    public record ErrorBody(String code, String message, List<ErrorDetail> details) {
    }

    public record ErrorDetail(String field, String message) {
    }
}

