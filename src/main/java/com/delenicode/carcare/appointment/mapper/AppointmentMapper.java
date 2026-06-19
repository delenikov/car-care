package com.delenicode.carcare.appointment.mapper;

import com.delenicode.carcare.appointment.dto.response.AppointmentResponse;
import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.service.AppointmentCancellationTokenService;
import com.delenicode.carcare.appointment.service.AppointmentTimePolicy;
import com.delenicode.carcare.vehicle.model.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentMapper {
  private final AppointmentTimePolicy timePolicy;
  private final AppointmentCancellationTokenService cancellationTokens;

  public AppointmentResponse toResponse(Appointment appointment) {
    Vehicle vehicle = appointment.getVehicle();
    return new AppointmentResponse(
        appointment.getId(),
        appointment.getCustomer().getId(),
        appointment.getCustomer().getFullName(),
        vehicle.getId(),
        vehicle.getPlateNumber(),
        vehicle.getMake() + " " + vehicle.getModel(),
        timePolicy.normalize(appointment.getScheduledAt()),
        timePolicy.normalize(appointment.getEndsAt()),
        appointment.getServiceType(),
        appointment.getStatus(),
        appointment.getNotes(),
        timePolicy.normalize(appointment.getCancellationExpiresAt()),
        cancellationTokens.cancellationUrl(appointment));
  }
}
