package com.delenicode.carcare.offer;

import com.delenicode.carcare.common.ValidationException;

public class InvalidOfferException extends ValidationException {
  public InvalidOfferException(String message) {
    super(message);
  }
}
