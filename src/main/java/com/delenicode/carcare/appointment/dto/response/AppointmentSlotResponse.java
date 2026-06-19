package com.delenicode.carcare.appointment.dto.response;


import com.delenicode.carcare.appointment.model.Appointment;
import java.time.OffsetDateTime;

public record AppointmentSlotResponse(OffsetDateTime startsAt, OffsetDateTime endsAt) {
}
