package com.delenicode.carcare.customer;

import com.delenicode.carcare.common.ResourceNotFoundException;

public class CustomerNotFoundException extends ResourceNotFoundException {
  public CustomerNotFoundException(Long id) {
    super("Customer not found: " + id);
  }
}
