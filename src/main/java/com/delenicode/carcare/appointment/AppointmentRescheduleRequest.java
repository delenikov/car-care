package com.delenicode.carcare.appointment;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record AppointmentRescheduleRequest(@NotNull OffsetDateTime startsAt, OffsetDateTime endsAt) {
}
