package com.delenicode.carcare.appointment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
  List<Appointment> findByScheduledAtBetweenOrderByScheduledAt(OffsetDateTime startsAt, OffsetDateTime endsAt);
  Optional<Appointment> findByCancellationToken(String cancellationToken);

  @Query("""
      select appointment from Appointment appointment
      where appointment.status <> com.delenicode.carcare.appointment.AppointmentStatus.CANCELLED
        and appointment.scheduledAt < :endsAt
        and appointment.endsAt > :startsAt
        and (:excludeId is null or appointment.id <> :excludeId)
      """)
  List<Appointment> findConflicts(@Param("startsAt") OffsetDateTime startsAt, @Param("endsAt") OffsetDateTime endsAt, @Param("excludeId") Long excludeId);

  @Query("""
      select appointment from Appointment appointment
      where appointment.status = com.delenicode.carcare.appointment.AppointmentStatus.SCHEDULED
        and appointment.scheduledAt >= :startsAt
        and appointment.scheduledAt < :endsAt
      order by appointment.scheduledAt
      """)
  List<Appointment> findReminderCandidates(@Param("startsAt") OffsetDateTime startsAt, @Param("endsAt") OffsetDateTime endsAt);
}
