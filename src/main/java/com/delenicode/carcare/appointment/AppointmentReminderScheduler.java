package com.delenicode.carcare.appointment;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Skopje");

  private final AppointmentService appointments;

  @Value("${app.appointments.reminders.enabled:true}")
  private boolean enabled;

  @Scheduled(cron = "${app.appointments.reminders.cron:0 0 8 * * *}", zone = "Europe/Skopje")
  void sendTomorrowReminders() {
    if (enabled) {
      appointments.sendReminders(LocalDate.now(BUSINESS_ZONE).plusDays(1));
    }
  }
}
