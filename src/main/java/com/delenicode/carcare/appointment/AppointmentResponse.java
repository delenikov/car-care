package com.delenicode.carcare.appointment;

import java.time.OffsetDateTime;

public record AppointmentResponse(
    Long id,
    Long customerId,
    Long vehicleId,
    OffsetDateTime scheduledAt,
    OffsetDateTime endsAt,
    String serviceType,
    AppointmentStatus status,
    String notes,
    OffsetDateTime cancellationExpiresAt,
    String cancellationUrl) {
}
