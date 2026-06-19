package com.delenicode.carcare.appointment.exception;

import com.delenicode.carcare.common.ResourceNotFoundException;

public class AppointmentNotFoundException extends ResourceNotFoundException {
  public AppointmentNotFoundException(Long id) {
    super("Appointment not found: " + id);
  }
}
