package com.delenicode.carcare.appointment.service;

import com.delenicode.carcare.appointment.exception.InvalidAppointmentException;
import com.delenicode.carcare.appointment.repository.AppointmentRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentConflictValidator {
  private final AppointmentRepository appointments
      ;

  public boolean hasConflict(OffsetDateTime startsAt, OffsetDateTime endsAt, Long excludeId) {
    return !appointments.findConflicts(startsAt, endsAt, excludeId).isEmpty();
  }

  public void rejectConflict(OffsetDateTime startsAt, OffsetDateTime endsAt, Long excludeId) {
    if (!hasConflict(startsAt, endsAt, excludeId)) {
      return;
    }
    log.warn("Appointment rejected. Reason: conflict. Start: {}. End: {}. Excluded appointment ID: {}", startsAt, endsAt, excludeId);
    throw new InvalidAppointmentException("Appointment conflicts with an existing appointment");
  }
}
