package com.delenicode.carcare.appointment.exception;

import com.delenicode.carcare.common.ValidationException;

public class InvalidAppointmentException extends ValidationException {
  public InvalidAppointmentException(String message) {
    super(message);
  }
}
