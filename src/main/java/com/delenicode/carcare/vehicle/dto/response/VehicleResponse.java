package com.delenicode.carcare.vehicle.dto.response;


import com.delenicode.carcare.vehicle.model.Vehicle;
public record VehicleResponse(
    Long id,
    Long customerId,
    String customerName,
    String plateNumber,
    String make,
    String model,
    Integer modelYear,
    String vin,
    String fuelType,
    String engine) {
}
