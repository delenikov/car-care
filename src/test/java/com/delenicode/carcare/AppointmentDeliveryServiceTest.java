package com.delenicode.carcare;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.model.AppointmentStatus;
import com.delenicode.carcare.appointment.repository.AppointmentRepository;
import com.delenicode.carcare.appointment.service.AppointmentCancellationTokenService;
import com.delenicode.carcare.appointment.service.AppointmentDeliveryService;
import com.delenicode.carcare.appointment.service.AppointmentEmailRenderer;
import com.delenicode.carcare.appointment.service.AppointmentTimePolicy;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.vehicle.model.Vehicle;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@ExtendWith(MockitoExtension.class)
class AppointmentDeliveryServiceTest {
  @Mock
  AppointmentRepository appointments;

  @Mock
  EmailService emailService;

  AppointmentDeliveryService deliveryService;
  TimeZone previousTimeZone;

  @BeforeEach
  void setUp() {
    previousTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Skopje"));
    AppointmentTimePolicy timePolicy = new AppointmentTimePolicy();
    AppointmentCancellationTokenService cancellationTokens = new AppointmentCancellationTokenService("http://localhost:5173");
    AppointmentEmailRenderer emailRenderer = new AppointmentEmailRenderer(templateEngine(), timePolicy, cancellationTokens);
    deliveryService = new AppointmentDeliveryService(appointments, emailService, emailRenderer);
  }

  @AfterEach
  void tearDown() {
    TimeZone.setDefault(previousTimeZone);
  }

  @Test
  void sendConfirmationLoadsAppointmentAndSendsHtmlEmail() {
    Appointment appointment = appointment();
    when(appointments.findByIdWithDetails(30L)).thenReturn(Optional.of(appointment));
    when(emailService.sendHtml(eq("ada@carcare.test"), eq("Потврда за термин"), contains("ПОТВРДА ЗА ТЕРМИН"), contains("/reservations/cancel/token")))
        .thenReturn(new EmailDeliveryResult("ada@carcare.test", "subject", true, "Email sent"));

    deliveryService.sendConfirmation(30L);

    verify(emailService).sendHtml(eq("ada@carcare.test"), eq("Потврда за термин"), contains("ПОТВРДА ЗА ТЕРМИН"), contains("/reservations/cancel/token"));
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

  private Appointment appointment() {
    Customer customer = new Customer();
    customer.setId(10L);
    customer.setFirstName("Ada");
    customer.setLastName("Lovelace");
    customer.setFullName("Ada Lovelace");
    customer.setEmail("ada@carcare.test");

    Vehicle vehicle = new Vehicle();
    vehicle.setId(20L);
    vehicle.setCustomer(customer);
    vehicle.setPlateNumber("SK-20");
    vehicle.setMake("Volkswagen");
    vehicle.setModel("Golf");

    Appointment appointment = new Appointment();
    appointment.setId(30L);
    appointment.setCustomer(customer);
    appointment.setVehicle(vehicle);
    appointment.setScheduledAt(OffsetDateTime.parse("2026-06-15T09:00:00+02:00"));
    appointment.setEndsAt(OffsetDateTime.parse("2026-06-15T10:00:00+02:00"));
    appointment.setServiceType("Minor Service");
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    appointment.setCancellationToken("token");
    return appointment;
  }
}
