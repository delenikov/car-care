package com.delenicode.carcare.common;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(error -> error.getField(), error -> String.valueOf(error.getDefaultMessage()), (a, b) -> a));
    log.warn("API validation failed. Path: {}. Field count: {}", LogSanitizer.path(request.getRequestURI()), errors.size());
    return ResponseEntity.badRequest().body(ApiErrorResponse.validation(HttpStatus.BAD_REQUEST.value(), "Validation failed", request.getRequestURI(), errors));
  }

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<ApiErrorResponse> badCredentials(BadCredentialsException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.UNAUTHORIZED;
    log.warn("API authentication failed. Path: {}. Message: {}", LogSanitizer.path(request.getRequestURI()), ex.getMessage());
    return ResponseEntity.status(status).body(ApiErrorResponse.of(status.value(), status.name(), ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiErrorResponse> apiException(ApiException ex, HttpServletRequest request) {
    log.warn("API exception handled. Path: {}. Status: {}. Error code: {}. Message: {}", LogSanitizer.path(request.getRequestURI()), ex.status().value(), ex.errorCode(), ex.getMessage());
    return ResponseEntity.status(ex.status()).body(ApiErrorResponse.of(ex.status().value(), ex.errorCode(), ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiErrorResponse> illegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    log.warn("API bad request. Path: {}. Message: {}", LogSanitizer.path(request.getRequestURI()), ex.getMessage());
    return ResponseEntity.badRequest().body(ApiErrorResponse.of(status.value(), "VALIDATION_ERROR", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> unexpected(Exception ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    log.error("Unexpected API error. Path: {}. Status: {}", LogSanitizer.path(request.getRequestURI()), status.value(), ex);
    return ResponseEntity.status(status)
        .body(ApiErrorResponse.of(status.value(), status.name(), "Unexpected server error", request.getRequestURI()));
  }
}
