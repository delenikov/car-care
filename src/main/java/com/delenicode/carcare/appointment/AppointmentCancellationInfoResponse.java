package com.delenicode.carcare.appointment;

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
