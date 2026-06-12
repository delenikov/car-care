package com.delenicode.carcare.appointment;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.vehicle.Vehicle;
import com.delenicode.carcare.vehicle.VehicleRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {
  private final AppointmentRepository appointments;
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;
  private final EmailService emailService;

  @Transactional(readOnly = true)
  public List<AppointmentResponse> findAll() {
    return appointments.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<AppointmentSlotResponse> availableSlots(LocalDate date) {
    OffsetDateTime dayStart = date.atTime(9, 0).atOffset(ZoneOffset.UTC);
    List<AppointmentSlotResponse> slots = new ArrayList<>();
    for (int hour = 9; hour < 17; hour++) {
      OffsetDateTime startsAt = date.atTime(hour, 0).atOffset(ZoneOffset.UTC);
      OffsetDateTime endsAt = startsAt.plusHours(1);
      if (appointments.findConflicts(startsAt, endsAt, null).isEmpty() && !startsAt.isBefore(dayStart)) {
        slots.add(new AppointmentSlotResponse(startsAt, endsAt));
      }
    }
    return slots;
  }

  @Transactional
  public AppointmentResponse create(AppointmentRequest request) {
    Customer customer = customers.findByIdAndDeletedFalse(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    Vehicle vehicle = vehicles.findById(request.vehicleId()).filter(existing -> !existing.getCustomer().isDeleted()).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
    if (!vehicle.getCustomer().getId().equals(customer.getId())) {
      throw new IllegalArgumentException("Vehicle does not belong to customer");
    }
    OffsetDateTime startsAt = startsAt(request);
    OffsetDateTime endsAt = endsAt(request, startsAt);
    rejectInvalidWindow(startsAt, endsAt);
    rejectConflict(startsAt, endsAt, null);
    Appointment appointment = new Appointment();
    appointment.setCustomer(customer);
    appointment.setVehicle(vehicle);
    appointment.setScheduledAt(startsAt);
    appointment.setEndsAt(endsAt);
    appointment.setServiceType(serviceType(request));
    appointment.setNotes(request.notes());
    appointment.setCancellationToken(UUID.randomUUID().toString());
    appointment.setCancellationExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(24));
    Appointment saved = appointments.save(appointment);
    sendConfirmation(saved);
    return toResponse(saved);
  }

  @Transactional
  public AppointmentResponse reschedule(Long id, AppointmentRescheduleRequest request) {
    Appointment appointment = appointments.findById(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
    OffsetDateTime startsAt = request.startsAt();
    OffsetDateTime endsAt = request.endsAt() == null ? startsAt.plusHours(1) : request.endsAt();
    rejectInvalidWindow(startsAt, endsAt);
    rejectConflict(startsAt, endsAt, id);
    appointment.setScheduledAt(startsAt);
    appointment.setEndsAt(endsAt);
    return toResponse(appointments.save(appointment));
  }

  @Transactional
  public AppointmentResponse cancelByToken(String token) {
    Appointment appointment = appointments.findByCancellationToken(token).orElseThrow(() -> new IllegalArgumentException("Cancellation link is invalid"));
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    if (appointment.getCancellationUsedAt() != null || appointment.getCancellationExpiresAt() == null || appointment.getCancellationExpiresAt().isBefore(now)) {
      throw new IllegalArgumentException("Cancellation link has expired");
    }
    appointment.setStatus(AppointmentStatus.CANCELLED);
    appointment.setCancellationUsedAt(now);
    return toResponse(appointments.save(appointment));
  }

  @Transactional
  public ReminderSummaryResponse sendReminders(LocalDate date) {
    OffsetDateTime startsAt = date.atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime endsAt = startsAt.plusDays(1);
    List<Appointment> candidates = appointments.findReminderCandidates(startsAt, endsAt);
    candidates.forEach(appointment -> emailService.send(appointment.getCustomer().getEmail(), "Appointment reminder", "Reminder for " + appointment.getServiceType() + " at " + appointment.getScheduledAt()));
    return new ReminderSummaryResponse(candidates.size());
  }

  public AppointmentResponse toResponse(Appointment appointment) {
    return new AppointmentResponse(appointment.getId(), appointment.getCustomer().getId(), appointment.getVehicle().getId(), appointment.getScheduledAt(), appointment.getEndsAt(), appointment.getServiceType(), appointment.getStatus(), appointment.getNotes(), appointment.getCancellationExpiresAt(), cancellationUrl(appointment));
  }

  private OffsetDateTime startsAt(AppointmentRequest request) {
    OffsetDateTime startsAt = request.startsAt() == null ? request.scheduledAt() : request.startsAt();
    if (startsAt == null) {
      throw new IllegalArgumentException("Appointment start is required");
    }
    return startsAt;
  }

  private OffsetDateTime endsAt(AppointmentRequest request, OffsetDateTime startsAt) {
    return request.endsAt() == null ? startsAt.plusHours(1) : request.endsAt();
  }

  private String serviceType(AppointmentRequest request) {
    String serviceType = request.serviceType() == null || request.serviceType().isBlank() ? request.title() : request.serviceType();
    if (serviceType == null || serviceType.isBlank()) {
      throw new IllegalArgumentException("Service type is required");
    }
    return serviceType;
  }

  private void rejectInvalidWindow(OffsetDateTime startsAt, OffsetDateTime endsAt) {
    if (!endsAt.isAfter(startsAt)) {
      throw new IllegalArgumentException("Appointment end must be after start");
    }
  }

  private void rejectConflict(OffsetDateTime startsAt, OffsetDateTime endsAt, Long excludeId) {
    if (!appointments.findConflicts(startsAt, endsAt, excludeId).isEmpty()) {
      throw new IllegalArgumentException("Appointment conflicts with an existing appointment");
    }
  }

  private void sendConfirmation(Appointment appointment) {
    emailService.send(appointment.getCustomer().getEmail(), "Appointment confirmation", "Your appointment for " + appointment.getServiceType() + " is scheduled at " + appointment.getScheduledAt() + ". Cancel: " + cancellationUrl(appointment));
  }

  private String cancellationUrl(Appointment appointment) {
    return appointment.getCancellationToken() == null ? null : "/api/appointments/cancel/" + appointment.getCancellationToken();
  }
}
