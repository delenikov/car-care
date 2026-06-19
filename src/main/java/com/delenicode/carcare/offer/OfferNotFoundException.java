package com.delenicode.carcare.offer;

import com.delenicode.carcare.common.ResourceNotFoundException;

public class OfferNotFoundException extends ResourceNotFoundException {
  public OfferNotFoundException(Long id) {
    super("Offer not found: " + id);
  }
}
