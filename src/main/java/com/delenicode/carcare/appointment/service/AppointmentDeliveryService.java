package com.delenicode.carcare.appointment.service;

import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.model.AppointmentEmail;
import com.delenicode.carcare.appointment.repository.AppointmentRepository;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentDeliveryService {
  private final AppointmentRepository appointments;
  private final EmailService emailService;
  private final AppointmentEmailRenderer emailRenderer;

  public void sendConfirmation(Long appointmentId) {
    appointments.findByIdWithDetails(appointmentId)
        .ifPresentOrElse(this::sendConfirmation, () -> log.warn("Appointment confirmation skipped. Appointment not found. Appointment ID: {}", appointmentId));
  }

  public boolean sendReminder(Appointment appointment) {
    EmailDeliveryResult result = emailService.send(
        appointment.getCustomer().getEmail(),
        emailRenderer.reminderSubject(),
        emailRenderer.renderReminderText(appointment));
    if (result == null || result.accepted()) {
      log.info("Appointment reminder sent. Appointment ID: {}. Customer ID: {}. Scheduled at: {}", appointment.getId(), appointment.getCustomer().getId(), appointment.getScheduledAt());
      return true;
    }
    log.warn("Appointment reminder failed. Appointment ID: {}. Customer ID: {}. Message: {}", appointment.getId(), appointment.getCustomer().getId(), result.message());
    return false;
  }

  private void sendConfirmation(Appointment appointment) {
    AppointmentEmail email = emailRenderer.renderConfirmation(appointment);
    EmailDeliveryResult result = emailService.sendHtml(
        appointment.getCustomer().getEmail(),
        email.subject(),
        email.htmlBody(),
        email.textBody());
    if (result == null || result.accepted()) {
      log.info("Appointment confirmation sent. Appointment ID: {}. Customer ID: {}", appointment.getId(), appointment.getCustomer().getId());
      return;
    }
    log.warn("Appointment confirmation failed. Appointment ID: {}. Customer ID: {}. Message: {}", appointment.getId(), appointment.getCustomer().getId(), result.message());
  }
}
