package org.salmanekhalili.jobtrack.dto;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape for the whole API. {@code fieldErrors} is populated
 * for validation failures and left empty otherwise, so clients can read one
 * shape without null checks.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ApiError of(Instant timestamp, int status, String error, String message, String path) {
        return new ApiError(timestamp, status, error, message, path, Map.of());
    }
}
