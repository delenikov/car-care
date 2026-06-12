package com.delenicode.carcare.appointment;

import java.time.OffsetDateTime;

public record AppointmentSlotResponse(OffsetDateTime startsAt, OffsetDateTime endsAt) {
}
