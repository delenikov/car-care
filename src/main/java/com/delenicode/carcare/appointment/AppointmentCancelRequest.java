package com.delenicode.carcare.appointment;

import jakarta.validation.constraints.NotBlank;

public record AppointmentCancelRequest(@NotBlank String token) {
}
