package com.delenicode.carcare.appointment.controller;


import com.delenicode.carcare.appointment.dto.request.AppointmentCancelRequest;
import com.delenicode.carcare.appointment.dto.request.AppointmentRequest;
import com.delenicode.carcare.appointment.dto.request.AppointmentRescheduleRequest;
import com.delenicode.carcare.appointment.dto.request.PublicAppointmentRequest;
import com.delenicode.carcare.appointment.dto.response.AppointmentCancellationInfoResponse;
import com.delenicode.carcare.appointment.dto.response.AppointmentResponse;
import com.delenicode.carcare.appointment.dto.response.AppointmentSlotResponse;
import com.delenicode.carcare.appointment.dto.response.ReminderSummaryResponse;
import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.service.AppointmentService;
import com.delenicode.carcare.common.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/appointments")
public class AppointmentController {
  private final AppointmentService appointments;

  @GetMapping
  ApiResponse<List<AppointmentResponse>> all() {
    return ApiResponse.ok("Appointments loaded", appointments.findAll());
  }

  @GetMapping("/available")
  ApiResponse<List<AppointmentSlotResponse>> available(@RequestParam LocalDate date) {
    return ApiResponse.ok("Available appointments loaded", appointments.availableSlots(date));
  }

  @PostMapping("/public")
  ApiResponse<AppointmentResponse> publicBooking(@Valid @RequestBody PublicAppointmentRequest request) {
    return ApiResponse.ok("Appointment created", appointments.createPublic(request));
  }

  @PostMapping
  ApiResponse<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request) {
    return ApiResponse.ok("Appointment created", appointments.create(request));
  }

  @PatchMapping("/{id}")
  ApiResponse<AppointmentResponse> reschedule(@PathVariable Long id, @Valid @RequestBody AppointmentRescheduleRequest request) {
    return ApiResponse.ok("Appointment rescheduled", appointments.reschedule(id, request));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Void> delete(@PathVariable Long id) {
    appointments.delete(id);
    return ApiResponse.ok("Appointment deleted", null);
  }

  @PostMapping("/cancel/{token}")
  ApiResponse<AppointmentResponse> cancel(@PathVariable String token) {
    return ApiResponse.ok("Appointment cancelled", appointments.cancelByToken(token));
  }

  @PostMapping("/cancel")
  ApiResponse<AppointmentResponse> cancelWithBody(@Valid @RequestBody AppointmentCancelRequest request) {
    return ApiResponse.ok("Appointment cancelled", appointments.cancelByToken(request.token()));
  }

  @GetMapping("/cancel-info/{token}")
  ApiResponse<AppointmentCancellationInfoResponse> cancellationInfo(@PathVariable String token) {
    return ApiResponse.ok("Appointment cancellation info loaded", appointments.cancellationInfo(token));
  }

  @PostMapping("/reminders")
  ApiResponse<ReminderSummaryResponse> sendReminders(@RequestParam LocalDate date) {
    return ApiResponse.ok("Appointment reminders sent", appointments.sendReminders(date));
  }
}
