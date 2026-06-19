package com.delenicode.carcare.servicerecord;

import com.delenicode.carcare.common.ResourceNotFoundException;

public class ServiceRecordNotFoundException extends ResourceNotFoundException {
  public ServiceRecordNotFoundException(Long id) {
    super("Service record not found: " + id);
  }
}
