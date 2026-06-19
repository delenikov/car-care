package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.appointment.dto.request.AppointmentRequest;
import com.delenicode.carcare.appointment.dto.request.PublicAppointmentRequest;
import com.delenicode.carcare.appointment.event.AppointmentCreatedEvent;
import com.delenicode.carcare.appointment.exception.InvalidAppointmentException;
import com.delenicode.carcare.appointment.mapper.AppointmentMapper;
import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.model.AppointmentStatus;
import com.delenicode.carcare.appointment.repository.AppointmentRepository;
import com.delenicode.carcare.appointment.service.AppointmentCancellationTokenService;
import com.delenicode.carcare.appointment.service.AppointmentConflictValidator;
import com.delenicode.carcare.appointment.service.AppointmentDeliveryService;
import com.delenicode.carcare.appointment.service.AppointmentEmailRenderer;
import com.delenicode.carcare.appointment.service.AppointmentService;
import com.delenicode.carcare.appointment.service.AppointmentTimePolicy;
import com.delenicode.carcare.appointment.service.PublicAppointmentBookingService;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.vehicle.model.Vehicle;
import com.delenicode.carcare.vehicle.repository.VehicleRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

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

  @Mock
  ApplicationEventPublisher events;

  AppointmentService appointmentService;
  TimeZone previousTimeZone;

  @BeforeEach
  void setUp() {
    previousTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Skopje"));
    AppointmentTimePolicy timePolicy = new AppointmentTimePolicy();
    AppointmentCancellationTokenService cancellationTokens = new AppointmentCancellationTokenService("http://localhost:5173");
    AppointmentMapper mapper = new AppointmentMapper(timePolicy, cancellationTokens);
    AppointmentConflictValidator conflictValidator = new AppointmentConflictValidator(appointments);
    PublicAppointmentBookingService publicBooking = new PublicAppointmentBookingService(customers, vehicles);
    AppointmentEmailRenderer emailRenderer = new AppointmentEmailRenderer(templateEngine(), timePolicy, cancellationTokens);
    AppointmentDeliveryService deliveryService = new AppointmentDeliveryService(appointments, emailService, emailRenderer);
    appointmentService = new AppointmentService(appointments, customers, vehicles, events, mapper, timePolicy, conflictValidator, cancellationTokens, publicBooking, deliveryService);
  }

  @AfterEach
  void tearDown() {
    TimeZone.setDefault(previousTimeZone);
  }

  @Test
  void createGeneratesCancellationLinkAndPublishesConfirmationEvent() {
    Customer customer = customer(10L);
    Vehicle vehicle = vehicle(20L, customer);
    OffsetDateTime startsAt = OffsetDateTime.parse("2026-06-15T09:00:00+02:00");
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(customer));
    when(vehicles.findById(20L)).thenReturn(Optional.of(vehicle));
    when(appointments.findConflicts(startsAt, startsAt.plusHours(1), null)).thenReturn(List.of());
    when(appointments.save(any(Appointment.class))).thenAnswer(invocation -> {
      Appointment appointment = invocation.getArgument(0);
      appointment.setId(30L);
      return appointment;
    });

    var response = appointmentService.create(new AppointmentRequest(10L, 20L, null, startsAt, startsAt.plusHours(1), null, "Minor Service", "Booking"));

    assertThat(response.cancellationUrl()).startsWith("http://localhost:5173/reservations/cancel/");
    assertThat(response.cancellationExpiresAt()).isAfter(OffsetDateTime.now().plusHours(23));
    verify(events).publishEvent(any(AppointmentCreatedEvent.class));
  }

  @Test
  void publicBookingCreatesCustomerVehicleAndConfirmation() {
    OffsetDateTime startsAt = OffsetDateTime.parse("2026-06-15T08:00:00+02:00");
    when(customers.findByEmailIgnoreCaseAndDeletedFalse("new@carcare.test")).thenReturn(Optional.empty());
    when(customers.save(any(Customer.class))).thenAnswer(invocation -> {
      Customer customer = invocation.getArgument(0);
      customer.setId(11L);
      return customer;
    });
    when(vehicles.findByPlateNumberIgnoreCaseAndCustomerDeletedFalse("SK-1234-AA")).thenReturn(Optional.empty());
    final Vehicle[] savedVehicle = new Vehicle[1];
    when(vehicles.save(any(Vehicle.class))).thenAnswer(invocation -> {
      Vehicle vehicle = invocation.getArgument(0);
      vehicle.setId(21L);
      savedVehicle[0] = vehicle;
      return vehicle;
    });
    when(appointments.findConflicts(startsAt, startsAt.plusHours(1), null)).thenReturn(List.of());
    when(appointments.save(any(Appointment.class))).thenAnswer(invocation -> {
      Appointment appointment = invocation.getArgument(0);
      appointment.setId(31L);
      return appointment;
    });

    var response = appointmentService.createPublic(new PublicAppointmentRequest("New Customer", "new@carcare.test", "+38970111111", "sk-1234-aa", "WAUZZZ8V0KA000001", "Audi", "A3", 2020, "1.6 TDI", "Diesel", startsAt, null, "Minor Service", "Public booking"));

    assertThat(response.customerName()).isEqualTo("New Customer");
    assertThat(response.vehiclePlate()).isEqualTo("SK-1234-AA");
    assertThat(response.vehicleName()).isEqualTo("Audi A3");
    assertThat(response.cancellationExpiresAt()).isAfter(OffsetDateTime.now().plusHours(23));
    assertThat(response.cancellationUrl()).startsWith("http://localhost:5173/reservations/cancel/");
    assertThat(savedVehicle[0].getVin()).isEqualTo("WAUZZZ8V0KA000001");
    assertThat(savedVehicle[0].getEngine()).isEqualTo("1.6 TDI");
    assertThat(savedVehicle[0].getFuelType()).isEqualTo("Diesel");
  }

  @Test
  void cancellationInfoDoesNotCancelAppointment() {
    Appointment appointment = appointment(30L, customer(10L), OffsetDateTime.parse("2026-06-15T09:00:00+02:00"));
    appointment.setCancellationToken("token");
    appointment.setCancellationExpiresAt(OffsetDateTime.now().plusHours(1));
    when(appointments.findByCancellationToken("token")).thenReturn(Optional.of(appointment));

    var response = appointmentService.cancellationInfo("token");

    assertThat(response.cancellable()).isTrue();
    assertThat(response.customerName()).isEqualTo("Ada Lovelace");
    assertThat(response.vehiclePlate()).isEqualTo("SK-20");
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
  }

  @Test
  void createRejectsConflictingAppointment() {
    Customer customer = customer(10L);
    Vehicle vehicle = vehicle(20L, customer);
    OffsetDateTime startsAt = OffsetDateTime.parse("2026-06-15T09:00:00+02:00");
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(customer));
    when(vehicles.findById(20L)).thenReturn(Optional.of(vehicle));
    when(appointments.findConflicts(startsAt, startsAt.plusHours(1), null)).thenReturn(List.of(new Appointment()));

    assertThatThrownBy(() -> appointmentService.create(new AppointmentRequest(10L, 20L, null, startsAt, startsAt.plusHours(1), "Minor Service", null, null)))
        .isInstanceOf(InvalidAppointmentException.class)
        .hasMessage("Appointment conflicts with an existing appointment");
  }

  @Test
  void cancelByTokenMarksLinkUsedAndAppointmentCancelled() {
    Appointment appointment = appointment(30L, customer(10L), OffsetDateTime.now().plusDays(1));
    appointment.setCancellationToken("token");
    appointment.setCancellationExpiresAt(OffsetDateTime.now().plusDays(2));
    when(appointments.findByCancellationToken("token")).thenReturn(Optional.of(appointment));
    when(appointments.save(appointment)).thenReturn(appointment);

    var response = appointmentService.cancelByToken("token");

    assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
    assertThat(appointment.getCancellationUsedAt()).isNotNull();
  }

  @Test
  void deleteRemovesExistingAppointment() {
    Appointment appointment = appointment(30L, customer(10L), OffsetDateTime.now().plusDays(1));
    when(appointments.findByIdWithDetails(30L)).thenReturn(Optional.of(appointment));

    appointmentService.delete(30L);

    verify(appointments).delete(appointment);
  }

  @Test
  void cancelByTokenRejectsExpiredOrUsedLink() {
    Appointment appointment = appointment(30L, customer(10L), OffsetDateTime.now().minusDays(1));
    appointment.setCancellationToken("token");
    appointment.setCancellationExpiresAt(OffsetDateTime.now().minusMinutes(1));
    when(appointments.findByCancellationToken("token")).thenReturn(Optional.of(appointment));

    assertThatThrownBy(() -> appointmentService.cancelByToken("token"))
        .isInstanceOf(InvalidAppointmentException.class)
        .hasMessage("Cancellation link has expired");
  }

  @Test
  void availableSlotsExcludeConflictingHours() {
    LocalDate date = LocalDate.of(2026, 6, 15);
    OffsetDateTime nine = OffsetDateTime.parse("2026-06-15T09:00:00+02:00");
    when(appointments.findConflicts(any(), any(), any())).thenAnswer(invocation -> {
      OffsetDateTime startsAt = invocation.getArgument(0);
      return startsAt.equals(nine) ? List.of(new Appointment()) : List.of();
    });

    var slots = appointmentService.availableSlots(date);

    assertThat(slots.get(0).startsAt()).isEqualTo(OffsetDateTime.parse("2026-06-15T08:00:00+02:00"));
    assertThat(slots.get(slots.size() - 1).endsAt()).isEqualTo(OffsetDateTime.parse("2026-06-15T16:00:00+02:00"));
    assertThat(slots).noneMatch(slot -> slot.startsAt().equals(nine));
    assertThat(slots).hasSize(7);
  }

  @Test
  void createRejectsAppointmentsOutsideBusinessHours() {
    Customer customer = customer(10L);
    Vehicle vehicle = vehicle(20L, customer);
    OffsetDateTime startsAt = OffsetDateTime.parse("2026-06-15T16:00:00+02:00");
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(customer));
    when(vehicles.findById(20L)).thenReturn(Optional.of(vehicle));

    assertThatThrownBy(() -> appointmentService.create(new AppointmentRequest(10L, 20L, null, startsAt, startsAt.plusHours(1), "Minor Service", null, null)))
        .isInstanceOf(InvalidAppointmentException.class)
        .hasMessage("Appointment must be between 08:00 and 16:00 Europe/Skopje");
  }

  @Test
  void sendRemindersEmailsMatchingAppointments() {
    Customer customer = customer(10L);
    Appointment appointment = appointment(30L, customer, OffsetDateTime.parse("2026-06-15T09:00:00+02:00"));
    when(appointments.findReminderCandidates(OffsetDateTime.parse("2026-06-15T00:00:00+02:00"), OffsetDateTime.parse("2026-06-16T00:00:00+02:00"))).thenReturn(List.of(appointment));
    when(emailService.send("ada@carcare.test", "Потсетник за термин", "Потсетник за Minor Service на 15.06.2026 во 09:00.")).thenReturn(new EmailDeliveryResult("ada@carcare.test", "Потсетник за термин", true, "Email sent"));

    var response = appointmentService.sendReminders(LocalDate.of(2026, 6, 15));

    assertThat(response.sent()).isEqualTo(1);
    assertThat(appointment.getReminderSentAt()).isNotNull();
    verify(emailService).send("ada@carcare.test", "Потсетник за термин", "Потсетник за Minor Service на 15.06.2026 во 09:00.");
    verify(appointments).save(appointment);
  }

  private TemplateEngine templateEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
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
