package com.delenicode.carcare.common;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> fieldErrors
) {
  public static ApiErrorResponse of(int status, String error, String message, String path) {
    return new ApiErrorResponse(Instant.now(), status, error, message, path, Map.of());
  }

  public static ApiErrorResponse validation(int status, String message, String path, Map<String, String> fieldErrors) {
    return new ApiErrorResponse(Instant.now(), status, "VALIDATION_ERROR", message, path, fieldErrors);
  }
}
