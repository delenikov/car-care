package com.delenicode.carcare.appointment.repository;


import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.model.AppointmentStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
  List<Appointment> findByScheduledAtBetweenOrderByScheduledAt(OffsetDateTime startsAt, OffsetDateTime endsAt);

  @EntityGraph(attributePaths = {"customer", "vehicle"})
  @Query("select appointment from Appointment appointment")
  List<Appointment> findAllWithDetails();

  @EntityGraph(attributePaths = {"customer", "vehicle"})
  @Query("select appointment from Appointment appointment where appointment.id = :id")
  Optional<Appointment> findByIdWithDetails(@Param("id") Long id);

  @EntityGraph(attributePaths = {"customer", "vehicle"})
  Optional<Appointment> findByCancellationToken(String cancellationToken);

  @Query("""
      select appointment from Appointment appointment
      where appointment.status <> com.delenicode.carcare.appointment.model.AppointmentStatus.CANCELLED
        and appointment.scheduledAt < :endsAt
        and appointment.endsAt > :startsAt
        and (:excludeId is null or appointment.id <> :excludeId)
      """)
  List<Appointment> findConflicts(@Param("startsAt") OffsetDateTime startsAt, @Param("endsAt") OffsetDateTime endsAt, @Param("excludeId") Long excludeId);

  @Query("""
      select appointment from Appointment appointment
      where appointment.status = com.delenicode.carcare.appointment.model.AppointmentStatus.SCHEDULED
        and appointment.reminderSentAt is null
        and appointment.scheduledAt >= :startsAt
        and appointment.scheduledAt < :endsAt
      order by appointment.scheduledAt
      """)
  @EntityGraph(attributePaths = {"customer", "vehicle"})
  List<Appointment> findReminderCandidates(@Param("startsAt") OffsetDateTime startsAt, @Param("endsAt") OffsetDateTime endsAt);
}
