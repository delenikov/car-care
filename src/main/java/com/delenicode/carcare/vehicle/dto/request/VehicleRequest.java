package com.delenicode.carcare.vehicle.dto.request;


import com.delenicode.carcare.vehicle.model.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleRequest(
    @NotNull Long customerId,
    @NotBlank String plateNumber,
    @NotBlank String make,
    @NotBlank String model,
    Integer modelYear,
    String vin,
    String fuelType,
    String engine) {
}
