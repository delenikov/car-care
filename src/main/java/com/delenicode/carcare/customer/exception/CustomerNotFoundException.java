package com.delenicode.carcare.customer.exception;


import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.common.ResourceNotFoundException;

public class CustomerNotFoundException extends ResourceNotFoundException {
  public CustomerNotFoundException(Long id) {
    super("Customer not found: " + id);
  }
}
