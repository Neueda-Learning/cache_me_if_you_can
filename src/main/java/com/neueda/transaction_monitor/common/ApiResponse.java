package com.neueda.transaction_monitor.common;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ApiResponse<T>(String message, T data, OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public record ErrorResponse(String message, OffsetDateTime timestamp) {
        public static ErrorResponse of(String message) {
            return new ErrorResponse(message, OffsetDateTime.now(ZoneOffset.UTC));
        }
    }
}
