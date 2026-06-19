package com.delenicode.carcare.offer.exception;


import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.common.ValidationException;

public class InvalidOfferException extends ValidationException {
  public InvalidOfferException(String message) {
    super(message);
  }
}
