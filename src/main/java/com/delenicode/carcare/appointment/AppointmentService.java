package com.delenicode.carcare.appointment;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.vehicle.Vehicle;
import com.delenicode.carcare.vehicle.VehicleRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Skopje");
  private static final LocalTime BUSINESS_START = LocalTime.of(8, 0);
  private static final LocalTime BUSINESS_END = LocalTime.of(16, 0);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  private final AppointmentRepository appointments;
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;
  private final EmailService emailService;

  @Value("${app.public-base-url:http://localhost:5173}")
  private String publicBaseUrl;

  @Transactional(readOnly = true)
  public List<AppointmentResponse> findAll() {
    return appointments.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<AppointmentSlotResponse> availableSlots(LocalDate date) {
    List<AppointmentSlotResponse> slots = new ArrayList<>();
    for (int hour = BUSINESS_START.getHour(); hour < BUSINESS_END.getHour(); hour++) {
      OffsetDateTime startsAt = atBusinessZone(date, hour);
      OffsetDateTime endsAt = startsAt.plusHours(1);
      if (appointments.findConflicts(startsAt, endsAt, null).isEmpty()) {
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
    OffsetDateTime startsAt = normalize(startsAt(request));
    OffsetDateTime endsAt = normalize(endsAt(request, startsAt));
    rejectInvalidWindow(startsAt, endsAt);
    rejectOutsideBusinessHours(startsAt, endsAt);
    rejectConflict(startsAt, endsAt, null);
    Appointment appointment = new Appointment();
    appointment.setCustomer(customer);
    appointment.setVehicle(vehicle);
    appointment.setScheduledAt(startsAt);
    appointment.setEndsAt(endsAt);
    appointment.setServiceType(serviceType(request));
    appointment.setNotes(request.notes());
    appointment.setCancellationToken(cancellationToken());
    appointment.setCancellationExpiresAt(cancellationExpiry());
    Appointment saved = appointments.save(appointment);
    sendConfirmation(saved);
    return toResponse(saved);
  }

  @Transactional
  public AppointmentResponse createPublic(PublicAppointmentRequest request) {
    Customer customer = customerForPublicBooking(request);
    Vehicle vehicle = vehicleForPublicBooking(request, customer);
    OffsetDateTime startsAt = normalize(request.startsAt());
    OffsetDateTime endsAt = normalize(request.endsAt() == null ? startsAt.plusHours(1) : request.endsAt());
    rejectInvalidWindow(startsAt, endsAt);
    rejectOutsideBusinessHours(startsAt, endsAt);
    rejectConflict(startsAt, endsAt, null);
    Appointment appointment = new Appointment();
    appointment.setCustomer(customer);
    appointment.setVehicle(vehicle);
    appointment.setScheduledAt(startsAt);
    appointment.setEndsAt(endsAt);
    appointment.setServiceType(request.serviceType());
    appointment.setNotes(request.notes());
    appointment.setCancellationToken(cancellationToken());
    appointment.setCancellationExpiresAt(cancellationExpiry());
    Appointment saved = appointments.save(appointment);
    sendConfirmation(saved);
    return toResponse(saved);
  }

  @Transactional
  public AppointmentResponse reschedule(Long id, AppointmentRescheduleRequest request) {
    Appointment appointment = appointments.findById(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
    OffsetDateTime startsAt = normalize(request.startsAt());
    OffsetDateTime endsAt = request.endsAt() == null ? startsAt.plusHours(1) : normalize(request.endsAt());
    rejectInvalidWindow(startsAt, endsAt);
    rejectOutsideBusinessHours(startsAt, endsAt);
    rejectConflict(startsAt, endsAt, id);
    appointment.setScheduledAt(startsAt);
    appointment.setEndsAt(endsAt);
    return toResponse(appointments.save(appointment));
  }

  @Transactional
  public AppointmentResponse cancelByToken(String token) {
    Appointment appointment = appointments.findByCancellationToken(token).orElseThrow(() -> new IllegalArgumentException("Cancellation link is invalid"));
    OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
    if (appointment.getCancellationUsedAt() != null || cancellationExpired(appointment, now)) {
      throw new IllegalArgumentException("Cancellation link has expired");
    }
    appointment.setStatus(AppointmentStatus.CANCELLED);
    appointment.setCancellationUsedAt(now);
    return toResponse(appointments.save(appointment));
  }

  @Transactional(readOnly = true)
  public AppointmentCancellationInfoResponse cancellationInfo(String token) {
    Appointment appointment = appointments.findByCancellationToken(token).orElseThrow(() -> new IllegalArgumentException("Cancellation link is invalid"));
    OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
    boolean cancellable = appointment.getCancellationUsedAt() == null
        && !cancellationExpired(appointment, now)
        && appointment.getStatus() != AppointmentStatus.CANCELLED;
    String message = cancellable ? "Терминот може да се откаже." : "Линкот за откажување е искористен или истечен.";
    Vehicle vehicle = appointment.getVehicle();
    return new AppointmentCancellationInfoResponse(
        appointment.getCustomer().getFullName(),
        vehicle.getPlateNumber(),
        vehicle.getMake() + " " + vehicle.getModel(),
        normalize(appointment.getScheduledAt()),
        normalize(appointment.getEndsAt()),
        appointment.getServiceType(),
        appointment.getStatus(),
        cancellable,
        message);
  }

  @Transactional
  public ReminderSummaryResponse sendReminders(LocalDate date) {
    OffsetDateTime startsAt = date.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
    OffsetDateTime endsAt = startsAt.plusDays(1);
    List<Appointment> candidates = appointments.findReminderCandidates(startsAt, endsAt);
    OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
    candidates.forEach(appointment -> {
      EmailDeliveryResult result = emailService.send(appointment.getCustomer().getEmail(), "Appointment reminder", "Reminder for " + appointment.getServiceType() + " at " + normalize(appointment.getScheduledAt()));
      if (result == null || result.accepted()) {
        appointment.setReminderSentAt(now);
      }
    });
    return new ReminderSummaryResponse(candidates.size());
  }

  public AppointmentResponse toResponse(Appointment appointment) {
    Vehicle vehicle = appointment.getVehicle();
    return new AppointmentResponse(
        appointment.getId(),
        appointment.getCustomer().getId(),
        appointment.getCustomer().getFullName(),
        vehicle.getId(),
        vehicle.getPlateNumber(),
        vehicle.getMake() + " " + vehicle.getModel(),
        normalize(appointment.getScheduledAt()),
        normalize(appointment.getEndsAt()),
        appointment.getServiceType(),
        appointment.getStatus(),
        appointment.getNotes(),
        normalize(appointment.getCancellationExpiresAt()),
        cancellationUrl(appointment));
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

  private void rejectOutsideBusinessHours(OffsetDateTime startsAt, OffsetDateTime endsAt) {
    LocalDate startDate = startsAt.atZoneSameInstant(BUSINESS_ZONE).toLocalDate();
    LocalDate endDate = endsAt.atZoneSameInstant(BUSINESS_ZONE).toLocalDate();
    LocalTime startTime = startsAt.atZoneSameInstant(BUSINESS_ZONE).toLocalTime();
    LocalTime endTime = endsAt.atZoneSameInstant(BUSINESS_ZONE).toLocalTime();
    if (!startDate.equals(endDate) || startTime.isBefore(BUSINESS_START) || endTime.isAfter(BUSINESS_END)) {
      throw new IllegalArgumentException("Appointment must be between 08:00 and 16:00 Europe/Skopje");
    }
  }

  private void rejectConflict(OffsetDateTime startsAt, OffsetDateTime endsAt, Long excludeId) {
    if (!appointments.findConflicts(startsAt, endsAt, excludeId).isEmpty()) {
      throw new IllegalArgumentException("Appointment conflicts with an existing appointment");
    }
  }

  private void sendConfirmation(Appointment appointment) {
    emailService.sendHtml(
        appointment.getCustomer().getEmail(),
        "Потврда за термин",
        confirmationHtml(appointment),
        confirmationText(appointment));
  }

  private String cancellationUrl(Appointment appointment) {
    return appointment.getCancellationToken() == null ? null : normalizedBaseUrl() + "/reservations/cancel/" + appointment.getCancellationToken();
  }

  private OffsetDateTime cancellationExpiry() {
    return OffsetDateTime.now(BUSINESS_ZONE).plusHours(24);
  }

  private boolean cancellationExpired(Appointment appointment, OffsetDateTime now) {
    OffsetDateTime expiresAt = appointment.getCancellationExpiresAt();
    return expiresAt == null || !expiresAt.isAfter(now);
  }

  private Customer customerForPublicBooking(PublicAppointmentRequest request) {
    return customers.findByEmailIgnoreCaseAndDeletedFalse(request.email().trim())
        .orElseGet(() -> {
          Customer customer = new Customer();
          String fullName = request.fullName().trim();
          customer.setFullName(fullName);
          customer.setFirstName(firstName(fullName));
          customer.setLastName(lastName(fullName));
          customer.setEmail(request.email().trim().toLowerCase());
          customer.setPhone(request.phone());
          return customers.save(customer);
        });
  }

  private Vehicle vehicleForPublicBooking(PublicAppointmentRequest request, Customer customer) {
    String plateNumber = request.plateNumber().trim().toUpperCase();
    return vehicles.findByPlateNumberIgnoreCaseAndCustomerDeletedFalse(plateNumber)
        .map(vehicle -> {
          if (!vehicle.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Vehicle plate already belongs to another customer");
          }
          return vehicle;
        })
        .orElseGet(() -> {
          Vehicle vehicle = new Vehicle();
          vehicle.setCustomer(customer);
          vehicle.setPlateNumber(plateNumber);
          vehicle.setVin(blankToNull(request.vin()));
          vehicle.setMake(request.make().trim());
          vehicle.setModel(request.model().trim());
          vehicle.setModelYear(request.modelYear());
          vehicle.setEngine(blankToNull(request.engine()));
          vehicle.setFuelType(blankToNull(request.fuelType()));
          return vehicles.save(vehicle);
        });
  }

  private String firstName(String fullName) {
    String[] parts = fullName.split("\\s+");
    return parts.length == 0 ? fullName : parts[0];
  }

  private String lastName(String fullName) {
    String[] parts = fullName.split("\\s+");
    return parts.length <= 1 ? firstName(fullName) : String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
  }

  private String normalizedBaseUrl() {
    String baseUrl = publicBaseUrl == null || publicBaseUrl.isBlank() ? "http://localhost:5173" : publicBaseUrl.trim();
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  private String confirmationText(Appointment appointment) {
    OffsetDateTime start = normalize(appointment.getScheduledAt());
    return "Почитувани " + appointment.getCustomer().getFullName()
        + ",\n\nВашиот термин за " + appointment.getServiceType() + " е закажан на "
        + DATE_FORMAT.format(start) + " во " + TIME_FORMAT.format(start) + "."
        + "\nВозило: " + vehicleLabel(appointment)
        + "\nОткажување: " + cancellationUrl(appointment);
  }

  private String confirmationHtml(Appointment appointment) {
    OffsetDateTime start = normalize(appointment.getScheduledAt());
    OffsetDateTime end = normalize(appointment.getEndsAt());
    return """
        <!doctype html>
        <html lang="mk">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Потврда за термин</title>
        </head>
        <body style="margin:0;padding:0;background:#f5efe2;font-family:Arial,Helvetica,sans-serif;color:#26322f;">
          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f5efe2;padding:32px 12px;">
            <tr>
              <td align="center">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:760px;background:#fffaf0;border:1px solid #e3d8c4;border-radius:8px;overflow:hidden;">
                  <tr>
                    <td style="padding:34px 38px 22px;background:#14231f;color:#fffaf0;">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                          <td style="vertical-align:top;">
                            <div style="font-size:22px;font-weight:700;color:#fffaf0;">CarCare ASMS</div>
                            <div style="font-size:13px;color:#d9cdb8;margin-top:8px;">Авто сервис центар</div>
                          </td>
                          <td align="right" style="vertical-align:top;">
                            <div style="font-size:26px;font-weight:700;color:#f2cf7a;">ПОТВРДА ЗА ТЕРМИН</div>
                            <div style="font-size:13px;color:#d9cdb8;margin-top:8px;">Бр. #%s</div>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:28px 38px 16px;">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                          <td width="50%%" style="vertical-align:top;padding-right:20px;">
                            <div style="font-size:12px;text-transform:uppercase;color:#7a6c58;font-weight:700;margin-bottom:8px;">Клиент</div>
                            <div style="font-size:16px;font-weight:700;color:#14231f;">%s</div>
                            <div style="font-size:14px;line-height:1.6;color:#5b635f;margin-top:6px;">%s</div>
                          </td>
                          <td width="50%%" style="vertical-align:top;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="font-size:14px;color:#5b635f;">
                              <tr><td style="padding:4px 0;">Датум:</td><td align="right" style="padding:4px 0;color:#14231f;font-weight:700;">%s</td></tr>
                              <tr><td style="padding:4px 0;">Време:</td><td align="right" style="padding:4px 0;color:#14231f;font-weight:700;">%s - %s</td></tr>
                              <tr><td style="padding:4px 0;">Возило:</td><td align="right" style="padding:4px 0;color:#14231f;font-weight:700;">%s</td></tr>
                            </table>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:14px 38px 0;">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;font-size:14px;">
                        <tr style="background:#f2cf7a;color:#14231f;">
                          <th align="left" style="padding:13px 12px;border-bottom:1px solid #d9a520;">Поле</th>
                          <th align="left" style="padding:13px 12px;border-bottom:1px solid #d9a520;">Информација</th>
                        </tr>
                        %s
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:26px 38px 36px;">
                      <div style="border-top:2px solid #d9a520;margin-bottom:20px;"></div>
                      <div style="font-size:14px;line-height:1.6;color:#5b635f;margin-bottom:18px;">Доколку треба да го откажете терминот, отворете го линкот подолу и потврдете го откажувањето.</div>
                      <a href="%s" style="display:inline-block;background:#14231f;color:#fffaf0;text-decoration:none;padding:12px 18px;border-radius:6px;font-weight:700;">Откажи термин</a>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.formatted(
        appointment.getId(),
        escape(appointment.getCustomer().getFullName()),
        escape(appointment.getCustomer().getEmail()),
        DATE_FORMAT.format(start),
        TIME_FORMAT.format(start),
        TIME_FORMAT.format(end),
        escape(vehicleLabel(appointment)),
        confirmationRows(appointment),
        escape(cancellationUrl(appointment))
    );
  }

  private String confirmationRows(Appointment appointment) {
    Vehicle vehicle = appointment.getVehicle();
    return row("Тип на сервис", appointment.getServiceType())
        + row("Регистарска табличка", vehicle.getPlateNumber())
        + row("VIN", blankToDash(vehicle.getVin()))
        + row("Мотор", blankToDash(vehicle.getEngine()))
        + row("Гориво", blankToDash(vehicle.getFuelType()))
        + row("Белешки", blankToDash(appointment.getNotes()));
  }

  private String row(String label, String value) {
    return """
        <tr>
          <td style="padding:14px 12px;border-bottom:1px solid #efe5d4;color:#5b635f;font-weight:700;">%s</td>
          <td style="padding:14px 12px;border-bottom:1px solid #efe5d4;color:#26322f;">%s</td>
        </tr>
        """.formatted(escape(label), escape(value));
  }

  private String vehicleLabel(Appointment appointment) {
    Vehicle vehicle = appointment.getVehicle();
    return vehicle.getPlateNumber() + " - " + vehicle.getMake() + " " + vehicle.getModel();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String blankToDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }

  private String escape(String value) {
    return blankToDash(value)
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private String cancellationToken() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private OffsetDateTime atBusinessZone(LocalDate date, int hour) {
    return date.atTime(hour, 0, 0).atZone(BUSINESS_ZONE).toOffsetDateTime();
  }

  private OffsetDateTime normalize(OffsetDateTime value) {
    return value == null ? null : value.atZoneSameInstant(BUSINESS_ZONE).toOffsetDateTime();
  }
}
