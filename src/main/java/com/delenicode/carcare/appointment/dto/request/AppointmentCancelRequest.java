package com.delenicode.carcare.appointment.dto.request;


import com.delenicode.carcare.appointment.model.Appointment;
import jakarta.validation.constraints.NotBlank;

public record AppointmentCancelRequest(@NotBlank String token) {
}
