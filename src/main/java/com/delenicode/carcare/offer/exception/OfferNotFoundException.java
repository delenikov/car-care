package com.delenicode.carcare.offer.exception;


import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.common.ResourceNotFoundException;

public class OfferNotFoundException extends ResourceNotFoundException {
  public OfferNotFoundException(Long id) {
    super("Offer not found: " + id);
  }
}
