package com.delenicode.carcare.appointment.service;

import com.delenicode.carcare.appointment.model.Appointment;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AppointmentCancellationTokenService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private final String publicBaseUrl;

  public AppointmentCancellationTokenService(@Value("${app.public-base-url:http://localhost:5173}") String publicBaseUrl) {
    this.publicBaseUrl = publicBaseUrl;
  }

  public String newToken() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public OffsetDateTime expiresAt() {
    return OffsetDateTime.now().plusHours(24);
  }

  public boolean isExpired(Appointment appointment, OffsetDateTime now) {
    OffsetDateTime expiresAt = appointment.getCancellationExpiresAt();
    return expiresAt == null || !expiresAt.isAfter(now);
  }

  public String cancellationUrl(Appointment appointment) {
    return appointment.getCancellationToken() == null ? null : normalizedBaseUrl() + "/reservations/cancel/" + appointment.getCancellationToken();
  }

  private String normalizedBaseUrl() {
    String baseUrl = publicBaseUrl == null || publicBaseUrl.isBlank() ? "http://localhost:5173" : publicBaseUrl.trim();
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
