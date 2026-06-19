package com.delenicode.carcare.appointment.service;

import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.model.AppointmentEmail;
import com.delenicode.carcare.vehicle.model.Vehicle;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class AppointmentEmailRenderer {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
  private static final Locale MK_LOCALE = Locale.forLanguageTag("mk-MK");

  private final TemplateEngine templateEngine;
  private final AppointmentTimePolicy timePolicy;
  private final AppointmentCancellationTokenService cancellationTokens;

  public AppointmentEmail renderConfirmation(Appointment appointment) {
    return new AppointmentEmail(confirmationSubject(), renderConfirmationHtml(appointment), renderConfirmationText(appointment));
  }

  public String confirmationSubject() {
    return "Потврда за термин";
  }

  public String reminderSubject() {
    return "Потсетник за термин";
  }

  public String renderReminderText(Appointment appointment) {
    OffsetDateTime start = timePolicy.normalize(appointment.getScheduledAt());
    return "Потсетник за " + appointment.getServiceType() + " на " + DATE_FORMAT.format(start) + " во " + TIME_FORMAT.format(start) + ".";
  }

  String renderConfirmationHtml(Appointment appointment) {
    Context context = new Context(MK_LOCALE);
    OffsetDateTime start = timePolicy.normalize(appointment.getScheduledAt());
    OffsetDateTime end = timePolicy.normalize(appointment.getEndsAt());
    Vehicle vehicle = appointment.getVehicle();
    context.setVariable("appointmentId", appointment.getId() == null ? "-" : appointment.getId());
    context.setVariable("customerName", appointment.getCustomer().getFullName());
    context.setVariable("customerEmail", appointment.getCustomer().getEmail());
    context.setVariable("appointmentDate", DATE_FORMAT.format(start));
    context.setVariable("startTime", TIME_FORMAT.format(start));
    context.setVariable("endTime", TIME_FORMAT.format(end));
    context.setVariable("vehicle", vehicleLabel(appointment));
    context.setVariable("serviceType", appointment.getServiceType());
    context.setVariable("plateNumber", vehicle.getPlateNumber());
    context.setVariable("vin", blankToDash(vehicle.getVin()));
    context.setVariable("engine", blankToDash(vehicle.getEngine()));
    context.setVariable("fuelType", blankToDash(vehicle.getFuelType()));
    context.setVariable("notes", blankToDash(appointment.getNotes()));
    context.setVariable("cancellationUrl", cancellationTokens.cancellationUrl(appointment));
    return templateEngine.process("appointment-confirmation-email", context);
  }

  String renderConfirmationText(Appointment appointment) {
    OffsetDateTime start = timePolicy.normalize(appointment.getScheduledAt());
    return """
        Почитувани %s,

        Вашиот термин за %s е закажан на %s во %s.
        Возило: %s
        Откажување: %s
        """.formatted(
        appointment.getCustomer().getFullName(),
        appointment.getServiceType(),
        DATE_FORMAT.format(start),
        TIME_FORMAT.format(start),
        vehicleLabel(appointment),
        cancellationTokens.cancellationUrl(appointment));
  }

  private String vehicleLabel(Appointment appointment) {
    Vehicle vehicle = appointment.getVehicle();
    return vehicle.getPlateNumber() + " - " + vehicle.getMake() + " " + vehicle.getModel();
  }

  private String blankToDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
