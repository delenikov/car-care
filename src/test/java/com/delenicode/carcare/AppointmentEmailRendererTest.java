package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;

import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.model.AppointmentEmail;
import com.delenicode.carcare.appointment.model.AppointmentStatus;
import com.delenicode.carcare.appointment.service.AppointmentCancellationTokenService;
import com.delenicode.carcare.appointment.service.AppointmentEmailRenderer;
import com.delenicode.carcare.appointment.service.AppointmentTimePolicy;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.vehicle.model.Vehicle;
import java.time.OffsetDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class AppointmentEmailRendererTest {
  TimeZone previousTimeZone;

  @BeforeEach
  void setUp() {
    previousTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Skopje"));
  }

  @AfterEach
  void tearDown() {
    TimeZone.setDefault(previousTimeZone);
  }

  @Test
  void rendersMacedonianHtmlAndPlainTextAppointmentEmail() {
    AppointmentEmailRenderer renderer = new AppointmentEmailRenderer(
        templateEngine(),
        new AppointmentTimePolicy(),
        new AppointmentCancellationTokenService("http://localhost:5173"));

    AppointmentEmail email = renderer.renderConfirmation(appointment());

    assertThat(email.subject()).isEqualTo("Потврда за термин");
    assertThat(email.htmlBody()).contains("ПОТВРДА ЗА ТЕРМИН", "CarCare", "Откажи термин", "15.06.2026");
    assertThat(email.textBody()).contains("Почитувани Ada Lovelace", "Вашиот термин за Minor Service е закажан на 15.06.2026 во 09:00.");
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
    customer.setFullName("Ada Lovelace");
    customer.setEmail("ada@carcare.test");

    Vehicle vehicle = new Vehicle();
    vehicle.setId(20L);
    vehicle.setCustomer(customer);
    vehicle.setPlateNumber("SK-20");
    vehicle.setMake("Volkswagen");
    vehicle.setModel("Golf");
    vehicle.setVin("WAUZZZ8V0KA000001");
    vehicle.setEngine("1.6 TDI");
    vehicle.setFuelType("Diesel");

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
