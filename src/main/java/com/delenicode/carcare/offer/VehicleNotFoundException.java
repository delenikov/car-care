package com.delenicode.carcare.offer;

import com.delenicode.carcare.common.ResourceNotFoundException;

public class VehicleNotFoundException extends ResourceNotFoundException {
  public VehicleNotFoundException(Long id) {
    super("Vehicle not found: " + id);
  }
}
