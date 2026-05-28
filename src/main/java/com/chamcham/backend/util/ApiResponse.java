package com.chamcham.backend.util;

public record ApiResponse<T>(boolean success, T data, Meta meta) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok(T data, Meta meta) {
        return new ApiResponse<>(true, data, meta);
    }

    public record Meta(int page, int limit, long total, int totalPages) {
    }
}

