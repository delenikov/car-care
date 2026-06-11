package com.delenicode.carcare.appointment;

import com.delenicode.carcare.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

  @PostMapping
  ApiResponse<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request) {
    return ApiResponse.ok("Appointment created", appointments.create(request));
  }
}
