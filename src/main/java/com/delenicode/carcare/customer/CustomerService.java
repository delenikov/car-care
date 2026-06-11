package com.delenicode.carcare.customer;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {
  private final CustomerRepository customers;

  @Transactional(readOnly = true)
  public List<CustomerResponse> findAll() {
    return customers.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public CustomerResponse findById(Long id) {
    return customers.findById(id).map(this::toResponse).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
  }

  @Transactional
  public CustomerResponse create(CustomerRequest request) {
    if (customers.existsByEmail(request.email())) {
      throw new IllegalArgumentException("Customer email already exists");
    }
    Customer customer = new Customer();
    apply(customer, request);
    return toResponse(customers.save(customer));
  }

  @Transactional
  public CustomerResponse update(Long id, CustomerRequest request) {
    Customer customer = customers.findById(id).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    apply(customer, request);
    return toResponse(customers.save(customer));
  }

  private void apply(Customer customer, CustomerRequest request) {
    customer.setFullName(request.fullName());
    customer.setEmail(request.email());
    customer.setPhone(request.phone());
    customer.setAddress(request.address());
  }

  public CustomerResponse toResponse(Customer customer) {
    return new CustomerResponse(customer.getId(), customer.getFullName(), customer.getEmail(), customer.getPhone(), customer.getAddress());
  }
}
