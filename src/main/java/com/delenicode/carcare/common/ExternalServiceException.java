package com.delenicode.carcare.common;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends ApiException {
  public ExternalServiceException(String message) {
    super(HttpStatus.UNPROCESSABLE_ENTITY, "EXTERNAL_SERVICE_ERROR", message);
  }

  public ExternalServiceException(String message, Throwable cause) {
    super(HttpStatus.UNPROCESSABLE_ENTITY, "EXTERNAL_SERVICE_ERROR", message, cause);
  }
}
