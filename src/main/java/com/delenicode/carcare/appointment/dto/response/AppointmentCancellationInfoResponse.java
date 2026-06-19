package com.delenicode.carcare.appointment.dto.response;


import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.model.AppointmentStatus;
import java.time.OffsetDateTime;

public record AppointmentCancellationInfoResponse(
    String customerName,
    String vehiclePlate,
    String vehicleName,
    OffsetDateTime scheduledAt,
    OffsetDateTime endsAt,
    String serviceType,
    AppointmentStatus status,
    boolean cancellable,
    String message) {
}
