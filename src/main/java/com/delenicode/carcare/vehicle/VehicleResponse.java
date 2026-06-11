package com.delenicode.carcare.vehicle;

public record VehicleResponse(Long id, Long customerId, String plateNumber, String make, String model, Integer modelYear, String vin) {
}
