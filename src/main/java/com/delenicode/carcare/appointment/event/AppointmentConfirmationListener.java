package com.delenicode.carcare.appointment.event;

import com.delenicode.carcare.appointment.service.AppointmentDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentConfirmationListener {
  private final AppointmentDeliveryService deliveryService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onAppointmentCreated(AppointmentCreatedEvent event) {
    log.info("Appointment created event received after commit. Appointment ID: {}", event.appointmentId());
    deliveryService.sendConfirmation(event.appointmentId());
  }
}
