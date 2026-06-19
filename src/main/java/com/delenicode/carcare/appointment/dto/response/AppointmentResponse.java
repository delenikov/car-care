package com.delenicode.carcare.appointment.dto.response;


import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.model.AppointmentStatus;
import java.time.OffsetDateTime;

public record AppointmentResponse(
    Long id,
    Long customerId,
    String customerName,
    Long vehicleId,
    String vehiclePlate,
    String vehicleName,
    OffsetDateTime scheduledAt,
    OffsetDateTime endsAt,
    String serviceType,
    AppointmentStatus status,
    String notes,
    OffsetDateTime cancellationExpiresAt,
    String cancellationUrl) {
}
