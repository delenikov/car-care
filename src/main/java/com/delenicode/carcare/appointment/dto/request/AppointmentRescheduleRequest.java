package com.delenicode.carcare.appointment.dto.request;


import com.delenicode.carcare.appointment.model.Appointment;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record AppointmentRescheduleRequest(@NotNull OffsetDateTime startsAt, OffsetDateTime endsAt) {
}
