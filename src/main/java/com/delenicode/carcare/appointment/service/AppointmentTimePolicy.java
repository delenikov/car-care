package com.delenicode.carcare.appointment.service;

import com.delenicode.carcare.appointment.dto.request.AppointmentRequest;
import com.delenicode.carcare.appointment.exception.InvalidAppointmentException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

@Service
public class AppointmentTimePolicy {
  private static final LocalTime BUSINESS_START = LocalTime.of(8, 0);
  private static final LocalTime BUSINESS_END = LocalTime.of(16, 0);

  public int firstBusinessHour() {
    return BUSINESS_START.getHour();
  }

  public int lastBusinessHourExclusive() {
    return BUSINESS_END.getHour();
  }

  public OffsetDateTime startsAt(AppointmentRequest request) {
    OffsetDateTime startsAt = request.startsAt() == null ? request.scheduledAt() : request.startsAt();
    if (startsAt == null) {
      throw new InvalidAppointmentException("Appointment start is required");
    }
    return startsAt;
  }

  public OffsetDateTime endsAt(AppointmentRequest request, OffsetDateTime startsAt) {
    return request.endsAt() == null ? startsAt.plusHours(1) : request.endsAt();
  }

  public void rejectInvalidWindow(OffsetDateTime startsAt, OffsetDateTime endsAt) {
    if (!endsAt.isAfter(startsAt)) {
      throw new InvalidAppointmentException("Appointment end must be after start");
    }
  }

  public void rejectOutsideBusinessHours(OffsetDateTime startsAt, OffsetDateTime endsAt) {
    LocalDate startDate = startsAt.atZoneSameInstant(businessZone()).toLocalDate();
    LocalDate endDate = endsAt.atZoneSameInstant(businessZone()).toLocalDate();
    LocalTime startTime = startsAt.atZoneSameInstant(businessZone()).toLocalTime();
    LocalTime endTime = endsAt.atZoneSameInstant(businessZone()).toLocalTime();
    if (!startDate.equals(endDate) || startTime.isBefore(BUSINESS_START) || endTime.isAfter(BUSINESS_END)) {
      throw new InvalidAppointmentException("Appointment must be between 08:00 and 16:00 " + businessZone().getId());
    }
  }

  public OffsetDateTime atBusinessZone(LocalDate date, int hour) {
    return date.atTime(hour, 0, 0).atZone(businessZone()).toOffsetDateTime();
  }

  public OffsetDateTime normalize(OffsetDateTime value) {
    return value == null ? null : value.atZoneSameInstant(businessZone()).toOffsetDateTime();
  }

  public ZoneId businessZone() {
    return ZoneId.systemDefault();
  }
}
