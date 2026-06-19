package com.delenicode.carcare.appointment.dto.request;


import com.delenicode.carcare.appointment.model.Appointment;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record AppointmentRequest(
    @NotNull Long customerId,
    @NotNull Long vehicleId,
    OffsetDateTime scheduledAt,
    OffsetDateTime startsAt,
    OffsetDateTime endsAt,
    String serviceType,
    String title,
    String notes) {
}
