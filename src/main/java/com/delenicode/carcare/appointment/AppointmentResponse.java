package com.delenicode.carcare.appointment;

import java.time.OffsetDateTime;

public record AppointmentResponse(Long id, Long customerId, Long vehicleId, OffsetDateTime scheduledAt, String serviceType, AppointmentStatus status, String notes) {
}
