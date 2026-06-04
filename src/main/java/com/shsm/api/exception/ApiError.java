package com.shsm.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        OffsetDateTime timestamp,
        List<String> details
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, OffsetDateTime.now(), null);
    }

    public static ApiError of(int status, String error, String message, String path,
                               List<String> details) {
        return new ApiError(status, error, message, path, OffsetDateTime.now(), details);
    }
}
