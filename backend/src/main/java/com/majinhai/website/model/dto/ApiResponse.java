package com.majinhai.website.model.dto;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        OffsetDateTime timestamp
) {

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(
                true,
                code,
                message,
                data,
                OffsetDateTime.now()
        );
    }

    public static <T> ApiResponse<T> failure(String code, String message, T data) {
        return new ApiResponse<>(
                false,
                code,
                message,
                data,
                OffsetDateTime.now()
        );
    }
}
