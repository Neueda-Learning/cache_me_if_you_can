package com.neueda.transaction_monitor.common;

import java.time.LocalDateTime;

public record ApiResponse<T>(String message, T data, LocalDateTime timestamp) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, LocalDateTime.now());
    }

    public record ErrorResponse(String message, LocalDateTime timestamp) {
        public static ErrorResponse of(String message) {
            return new ErrorResponse(message, LocalDateTime.now());
        }
    }
}
