package com.digiteen.userservice.error;

import java.time.Instant;
import java.util.Map;


public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String traceId,
        Map<String, String> fieldErrors) {
}
