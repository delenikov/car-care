package com.delenicode.carcare.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record AppointmentRequest(@NotNull Long customerId, @NotNull Long vehicleId, @NotNull OffsetDateTime scheduledAt, @NotBlank String serviceType, String notes) {
}
