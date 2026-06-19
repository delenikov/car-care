package com.delenicode.carcare.offer;

import com.delenicode.carcare.common.ExternalServiceException;

public class OfferDeliveryException extends ExternalServiceException {
  public OfferDeliveryException(String message) {
    super(message);
  }

  public OfferDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
