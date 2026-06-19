package com.delenicode.carcare.servicerecord.exception;


import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import com.delenicode.carcare.common.ValidationException;

public class InvalidServiceRecordException extends ValidationException {
  public InvalidServiceRecordException(String message) {
    super(message);
  }
}
