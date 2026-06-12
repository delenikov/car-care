package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.appointment.Appointment;
import com.delenicode.carcare.appointment.AppointmentRepository;
import com.delenicode.carcare.appointment.AppointmentRequest;
import com.delenicode.carcare.appointment.AppointmentService;
import com.delenicode.carcare.appointment.AppointmentStatus;
import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.vehicle.Vehicle;
import com.delenicode.carcare.vehicle.VehicleRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {
  @Mock
  AppointmentRepository appointments;

  @Mock
  CustomerRepository customers;

  @Mock
  VehicleRepository vehicles;

  @Mock
  EmailService emailService;

  AppointmentService appointmentService;

  @BeforeEach
  void setUp() {
    appointmentService = new AppointmentService(appointments, customers, vehicles, emailService);
  }

  @Test
  void createGeneratesCancellationLinkAndSendsConfirmation() {
    Customer customer = customer(10L);
    Vehicle vehicle = vehicle(20L, customer);
    OffsetDateTime startsAt = OffsetDateTime.parse("2026-06-15T09:00:00Z");
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(customer));
    when(vehicles.findById(20L)).thenReturn(Optional.of(vehicle));
    when(appointments.findConflicts(startsAt, startsAt.plusHours(1), null)).thenReturn(List.of());
    when(appointments.save(any(Appointment.class))).thenAnswer(invocation -> {
      Appointment appointment = invocation.getArgument(0);
      appointment.setId(30L);
      return appointment;
    });

    var response = appointmentService.create(new AppointmentRequest(10L, 20L, null, startsAt, startsAt.plusHours(1), null, "Minor Service", "Booking"));

    assertThat(response.cancellationUrl()).startsWith("/api/appointments/cancel/");
    assertThat(response.cancellationExpiresAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusHours(23));
    verify(emailService).send("ada@carcare.test", "Appointment confirmation", "Your appointment for Minor Service is scheduled at 2026-06-15T09:00Z. Cancel: " + response.cancellationUrl());
  }

  @Test
  void createRejectsConflictingAppointment() {
    Customer customer = customer(10L);
    Vehicle vehicle = vehicle(20L, customer);
    OffsetDateTime startsAt = OffsetDateTime.parse("2026-06-15T09:00:00Z");
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(customer));
    when(vehicles.findById(20L)).thenReturn(Optional.of(vehicle));
    when(appointments.findConflicts(startsAt, startsAt.plusHours(1), null)).thenReturn(List.of(new Appointment()));

    assertThatThrownBy(() -> appointmentService.create(new AppointmentRequest(10L, 20L, null, startsAt, startsAt.plusHours(1), "Minor Service", null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Appointment conflicts with an existing appointment");
  }

  @Test
  void cancelByTokenMarksLinkUsedAndAppointmentCancelled() {
    Appointment appointment = appointment(30L, customer(10L), OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
    appointment.setCancellationToken("token");
    appointment.setCancellationExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
    when(appointments.findByCancellationToken("token")).thenReturn(Optional.of(appointment));
    when(appointments.save(appointment)).thenReturn(appointment);

    var response = appointmentService.cancelByToken("token");

    assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
    assertThat(appointment.getCancellationUsedAt()).isNotNull();
  }

  @Test
  void cancelByTokenRejectsExpiredOrUsedLink() {
    Appointment appointment = appointment(30L, customer(10L), OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
    appointment.setCancellationToken("token");
    appointment.setCancellationExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
    when(appointments.findByCancellationToken("token")).thenReturn(Optional.of(appointment));

    assertThatThrownBy(() -> appointmentService.cancelByToken("token"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cancellation link has expired");
  }

  @Test
  void availableSlotsExcludeConflictingHours() {
    LocalDate date = LocalDate.of(2026, 6, 15);
    OffsetDateTime nine = date.atTime(9, 0).atOffset(ZoneOffset.UTC);
    when(appointments.findConflicts(nine, nine.plusHours(1), null)).thenReturn(List.of(new Appointment()));

    var slots = appointmentService.availableSlots(date);

    assertThat(slots).noneMatch(slot -> slot.startsAt().equals(nine));
    assertThat(slots).hasSize(7);
  }

  @Test
  void sendRemindersEmailsMatchingAppointments() {
    Customer customer = customer(10L);
    Appointment appointment = appointment(30L, customer, OffsetDateTime.parse("2026-06-15T09:00:00Z"));
    when(appointments.findReminderCandidates(OffsetDateTime.parse("2026-06-15T00:00:00Z"), OffsetDateTime.parse("2026-06-16T00:00:00Z"))).thenReturn(List.of(appointment));

    var response = appointmentService.sendReminders(LocalDate.of(2026, 6, 15));

    assertThat(response.sent()).isEqualTo(1);
    verify(emailService).send("ada@carcare.test", "Appointment reminder", "Reminder for Minor Service at 2026-06-15T09:00Z");
  }

  private Customer customer(Long id) {
    Customer customer = new Customer();
    customer.setId(id);
    customer.setFirstName("Ada");
    customer.setLastName("Lovelace");
    customer.setFullName("Ada Lovelace");
    customer.setEmail("ada@carcare.test");
    return customer;
  }

  private Vehicle vehicle(Long id, Customer customer) {
    Vehicle vehicle = new Vehicle();
    vehicle.setId(id);
    vehicle.setCustomer(customer);
    vehicle.setPlateNumber("SK-" + id);
    vehicle.setMake("Volkswagen");
    vehicle.setModel("Golf");
    vehicle.setModelYear(2020);
    return vehicle;
  }

  private Appointment appointment(Long id, Customer customer, OffsetDateTime startsAt) {
    Appointment appointment = new Appointment();
    appointment.setId(id);
    appointment.setCustomer(customer);
    appointment.setVehicle(vehicle(20L, customer));
    appointment.setScheduledAt(startsAt);
    appointment.setEndsAt(startsAt.plusHours(1));
    appointment.setServiceType("Minor Service");
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    return appointment;
  }
}
