package com.delenicode.carcare.appointment.dto.request;


import com.delenicode.carcare.appointment.model.Appointment;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record PublicAppointmentRequest(
    @NotBlank String fullName,
    @NotBlank @Email String email,
    String phone,
    @NotBlank String plateNumber,
    String vin,
    @NotBlank String make,
    @NotBlank String model,
    Integer modelYear,
    String engine,
    String fuelType,
    @NotNull OffsetDateTime startsAt,
    OffsetDateTime endsAt,
    @NotBlank String serviceType,
    String notes) {
}
