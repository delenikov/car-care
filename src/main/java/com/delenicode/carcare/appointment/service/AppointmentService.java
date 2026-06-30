package com.delenicode.carcare.appointment.service;

import com.delenicode.carcare.appointment.dto.request.AppointmentRequest;
import com.delenicode.carcare.appointment.dto.request.AppointmentRescheduleRequest;
import com.delenicode.carcare.appointment.dto.request.PublicAppointmentRequest;
import com.delenicode.carcare.appointment.dto.response.AppointmentCancellationInfoResponse;
import com.delenicode.carcare.appointment.dto.response.AppointmentResponse;
import com.delenicode.carcare.appointment.dto.response.AppointmentSlotResponse;
import com.delenicode.carcare.appointment.dto.response.ReminderSummaryResponse;
import com.delenicode.carcare.appointment.event.AppointmentCreatedEvent;
import com.delenicode.carcare.appointment.exception.AppointmentNotFoundException;
import com.delenicode.carcare.appointment.exception.InvalidAppointmentException;
import com.delenicode.carcare.appointment.mapper.AppointmentMapper;
import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.model.AppointmentStatus;
import com.delenicode.carcare.appointment.repository.AppointmentRepository;
import com.delenicode.carcare.common.LogSanitizer;
import com.delenicode.carcare.customer.exception.CustomerNotFoundException;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import com.delenicode.carcare.vehicle.exception.VehicleNotFoundException;
import com.delenicode.carcare.vehicle.model.Vehicle;
import com.delenicode.carcare.vehicle.repository.VehicleRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {
  private static final String CANCELLATION_ALLOWED_MESSAGE = "Терминот може да се откаже.";
  private static final String CANCELLATION_UNAVAILABLE_MESSAGE = "Линкот за откажување е искористен или истечен.";

  private final AppointmentRepository appointments;
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;
  private final ApplicationEventPublisher events;
  private final AppointmentMapper mapper;
  private final AppointmentTimePolicy timePolicy;
  private final AppointmentConflictValidator conflictValidator;
  private final AppointmentCancellationTokenService cancellationTokens;
  private final PublicAppointmentBookingService publicBooking;
  private final AppointmentDeliveryService deliveryService;

  @Transactional(readOnly = true)
  public List<AppointmentResponse> findAll() {
    return appointments.findAllWithDetails().stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<AppointmentSlotResponse> availableSlots(LocalDate date) {
    List<AppointmentSlotResponse> slots = new ArrayList<>();
    OffsetDateTime now = OffsetDateTime.now(timePolicy.businessZone());
    for (int hour = timePolicy.firstBusinessHour(); hour < timePolicy.lastBusinessHourExclusive(); hour++) {
      OffsetDateTime startsAt = timePolicy.atBusinessZone(date, hour);
      OffsetDateTime endsAt = startsAt.plusHours(1);
      if (startsAt.isBefore(now)) {
        continue;
      }
      if (!conflictValidator.hasConflict(startsAt, endsAt, null)) {
        slots.add(new AppointmentSlotResponse(startsAt, endsAt));
      }
    }
    return slots;
  }

  @Transactional
  public AppointmentResponse create(AppointmentRequest request) {
    Customer customer = customers.findByIdAndDeletedFalse(request.customerId()).orElseThrow(() -> new CustomerNotFoundException(request.customerId()));
    Vehicle vehicle = resolveVehicle(request.vehicleId(), customer);
    OffsetDateTime startsAt = timePolicy.normalize(timePolicy.startsAt(request));
    OffsetDateTime endsAt = timePolicy.normalize(timePolicy.endsAt(request, startsAt));
    validateSchedule(startsAt, endsAt, null);

    Appointment saved = appointments.save(newAppointment(customer, vehicle, startsAt, endsAt, serviceType(request), request.notes()));
    log.info(
        "Appointment created by admin. Appointment ID: {}. Customer ID: {}. Vehicle ID: {}. Start: {}. End: {}. Status: {}",
        saved.getId(),
        customer.getId(),
        vehicle.getId(),
        timePolicy.normalize(saved.getScheduledAt()),
        timePolicy.normalize(saved.getEndsAt()),
        saved.getStatus());
    publishCreated(saved);
    return mapper.toResponse(saved);
  }

  @Transactional
  public AppointmentResponse createPublic(PublicAppointmentRequest request) {
    Customer customer = publicBooking.resolveCustomer(request);
    Vehicle vehicle = publicBooking.resolveVehicle(request, customer);
    OffsetDateTime startsAt = timePolicy.normalize(request.startsAt());
    OffsetDateTime endsAt = timePolicy.normalize(request.endsAt() == null ? startsAt.plusHours(1) : request.endsAt());
    validateSchedule(startsAt, endsAt, null);

    Appointment saved = appointments.save(newAppointment(customer, vehicle, startsAt, endsAt, serviceType(request.serviceType()), request.notes()));
    log.info(
        "Appointment created by public booking. Appointment ID: {}. Customer ID: {}. Vehicle ID: {}. Start: {}. End: {}. Status: {}. Email: {}",
        saved.getId(),
        customer.getId(),
        vehicle.getId(),
        timePolicy.normalize(saved.getScheduledAt()),
        timePolicy.normalize(saved.getEndsAt()),
        saved.getStatus(),
        LogSanitizer.email(customer.getEmail()));
    publishCreated(saved);
    return mapper.toResponse(saved);
  }

  @Transactional
  public AppointmentResponse reschedule(Long id, AppointmentRescheduleRequest request) {
    Appointment appointment = appointments.findByIdWithDetails(id).orElseThrow(() -> new AppointmentNotFoundException(id));
    OffsetDateTime startsAt = timePolicy.normalize(request.startsAt());
    OffsetDateTime endsAt = request.endsAt() == null ? startsAt.plusHours(1) : timePolicy.normalize(request.endsAt());
    validateSchedule(startsAt, endsAt, id);

    OffsetDateTime previousStart = appointment.getScheduledAt();
    OffsetDateTime previousEnd = appointment.getEndsAt();
    appointment.setScheduledAt(startsAt);
    appointment.setEndsAt(endsAt);
    Appointment saved = appointments.save(appointment);
    log.info(
        "Appointment rescheduled. Appointment ID: {}. Previous start: {}. Previous end: {}. New start: {}. New end: {}",
        saved.getId(),
        timePolicy.normalize(previousStart),
        timePolicy.normalize(previousEnd),
        timePolicy.normalize(saved.getScheduledAt()),
        timePolicy.normalize(saved.getEndsAt()));
    return mapper.toResponse(saved);
  }

  @Transactional
  public void delete(Long id) {
    Appointment appointment = appointments.findByIdWithDetails(id).orElseThrow(() -> new AppointmentNotFoundException(id));
    appointments.delete(appointment);
    log.info("Appointment deleted. Appointment ID: {}. Customer ID: {}. Vehicle ID: {}", appointment.getId(), appointment.getCustomer().getId(), appointment.getVehicle().getId());
  }

  @Transactional
  public AppointmentResponse cancelByToken(String token) {
    Appointment appointment = appointments.findByCancellationToken(token).orElseThrow(() -> new InvalidAppointmentException("Cancellation link is invalid"));
    OffsetDateTime now = OffsetDateTime.now();
    if (appointment.getCancellationUsedAt() != null || cancellationTokens.isExpired(appointment, now)) {
      log.warn("Appointment cancellation rejected. Reason: expired or already used. Appointment ID: {}. Status: {}", appointment.getId(), appointment.getStatus());
      throw new InvalidAppointmentException("Cancellation link has expired");
    }
    appointment.setStatus(AppointmentStatus.CANCELLED);
    appointment.setCancellationUsedAt(now);
    Appointment saved = appointments.save(appointment);
    log.info("Appointment cancelled by public link. Appointment ID: {}. Customer ID: {}. Vehicle ID: {}", saved.getId(), saved.getCustomer().getId(), saved.getVehicle().getId());
    return mapper.toResponse(saved);
  }

  @Transactional(readOnly = true)
  public AppointmentCancellationInfoResponse cancellationInfo(String token) {
    Appointment appointment = appointments.findByCancellationToken(token).orElseThrow(() -> new InvalidAppointmentException("Cancellation link is invalid"));
    boolean cancellable = appointment.getCancellationUsedAt() == null
        && !cancellationTokens.isExpired(appointment, OffsetDateTime.now())
        && appointment.getStatus() != AppointmentStatus.CANCELLED;
    Vehicle vehicle = appointment.getVehicle();
    return new AppointmentCancellationInfoResponse(
        appointment.getCustomer().getFullName(),
        vehicle.getPlateNumber(),
        vehicle.getMake() + " " + vehicle.getModel(),
        timePolicy.normalize(appointment.getScheduledAt()),
        timePolicy.normalize(appointment.getEndsAt()),
        appointment.getServiceType(),
        appointment.getStatus(),
        cancellable,
        cancellationMessage(cancellable));
  }

  public ReminderSummaryResponse sendReminders(LocalDate date) {
    OffsetDateTime startsAt = date.atStartOfDay(timePolicy.businessZone()).toOffsetDateTime();
    OffsetDateTime endsAt = startsAt.plusDays(1);
    List<Appointment> candidates = appointments.findReminderCandidates(startsAt, endsAt);
    OffsetDateTime now = OffsetDateTime.now();
    int sent = 0;
    for (Appointment appointment : candidates) {
      if (deliveryService.sendReminder(appointment)) {
        appointment.setReminderSentAt(now);
        appointments.save(appointment);
        sent++;
      }
    }
    return new ReminderSummaryResponse(sent);
  }

  private Vehicle resolveVehicle(Long vehicleId, Customer customer) {
    Vehicle vehicle = vehicles.findById(vehicleId)
        .filter(existing -> !existing.getCustomer().isDeleted())
        .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
    if (!Objects.equals(vehicle.getCustomer().getId(), customer.getId())) {
      throw new InvalidAppointmentException("Vehicle does not belong to customer");
    }
    return vehicle;
  }

  private void validateSchedule(OffsetDateTime startsAt, OffsetDateTime endsAt, Long excludeId) {
    timePolicy.rejectInvalidWindow(startsAt, endsAt);
    timePolicy.rejectOutsideBusinessHours(startsAt, endsAt);
    conflictValidator.rejectConflict(startsAt, endsAt, excludeId);
  }

  private Appointment newAppointment(Customer customer, Vehicle vehicle, OffsetDateTime startsAt, OffsetDateTime endsAt, String serviceType, String notes) {
    Appointment appointment = new Appointment();
    appointment.setCustomer(customer);
    appointment.setVehicle(vehicle);
    appointment.setScheduledAt(startsAt);
    appointment.setEndsAt(endsAt);
    appointment.setServiceType(serviceType);
    appointment.setNotes(notes);
    appointment.setCancellationToken(cancellationTokens.newToken());
    appointment.setCancellationExpiresAt(cancellationTokens.expiresAt());
    return appointment;
  }

  private String serviceType(AppointmentRequest request) {
    String serviceType = request.serviceType() == null || request.serviceType().isBlank() ? request.title() : request.serviceType();
    return serviceType(serviceType);
  }

  private String serviceType(String serviceType) {
    if (serviceType == null || serviceType.isBlank()) {
      throw new InvalidAppointmentException("Service type is required");
    }
    return serviceType;
  }

  private void publishCreated(Appointment appointment) {
    log.info("Appointment created event published. Appointment ID: {}", appointment.getId());
    events.publishEvent(new AppointmentCreatedEvent(appointment.getId()));
  }

  private String cancellationMessage(boolean cancellable) {
    return cancellable ? CANCELLATION_ALLOWED_MESSAGE : CANCELLATION_UNAVAILABLE_MESSAGE;
  }
}
