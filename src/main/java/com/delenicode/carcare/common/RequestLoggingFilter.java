package com.delenicode.carcare.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final String CORRELATION_ID = "correlationId";
  private static final String CORRELATION_HEADER = "X-Correlation-Id";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = correlationId(request);
    MDC.put(CORRELATION_ID, correlationId);
    response.setHeader(CORRELATION_HEADER, correlationId);
    long startedAt = System.nanoTime();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
      log.info(
          "HTTP request completed. Method: {}. Path: \"{}\". Status: {}. Duration: {} ms. Remote address: {}",
          request.getMethod(),
          LogSanitizer.path(request.getRequestURI()),
          response.getStatus(),
          durationMs,
          request.getRemoteAddr());
      MDC.remove(CORRELATION_ID);
    }
  }

  private String correlationId(HttpServletRequest request) {
    String existing = request.getHeader(CORRELATION_HEADER);
    return existing == null || existing.isBlank() ? UUID.randomUUID().toString() : existing.trim();
  }
}
