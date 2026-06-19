package com.delenicode.carcare.vehicle.exception;


import com.delenicode.carcare.vehicle.model.Vehicle;
import com.delenicode.carcare.common.ResourceNotFoundException;

public class VehicleNotFoundException extends ResourceNotFoundException {
  public VehicleNotFoundException(Long id) {
    super("Vehicle not found: " + id);
  }
}
