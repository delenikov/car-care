package com.delenicode.carcare.servicerecord;

import com.delenicode.carcare.common.ValidationException;

public class InvalidServiceRecordException extends ValidationException {
  public InvalidServiceRecordException(String message) {
    super(message);
  }
}
