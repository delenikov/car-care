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
    return customers.findByDeletedFalse().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<CustomerResponse> search(String firstName, String lastName) {
    if (firstName != null && !firstName.isBlank()) {
      return customers.findByFirstNameContainingIgnoreCaseAndDeletedFalse(firstName).stream().map(this::toResponse).toList();
    }
    if (lastName != null && !lastName.isBlank()) {
      return customers.findByLastNameContainingIgnoreCaseAndDeletedFalse(lastName).stream().map(this::toResponse).toList();
    }
    return findAll();
  }

  @Transactional(readOnly = true)
  public CustomerResponse findById(Long id) {
    return customers.findByIdAndDeletedFalse(id).map(this::toResponse).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
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
    Customer customer = customers.findByIdAndDeletedFalse(id).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    apply(customer, request);
    return toResponse(customers.save(customer));
  }

  @Transactional
  public void delete(Long id) {
    Customer customer = customers.findByIdAndDeletedFalse(id).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    customer.setDeleted(true);
    customers.save(customer);
  }

  private void apply(Customer customer, CustomerRequest request) {
    NameParts name = names(request);
    customer.setFirstName(name.firstName());
    customer.setLastName(name.lastName());
    customer.setFullName(name.fullName());
    customer.setEmail(request.email());
    customer.setPhone(request.phone());
    customer.setAddress(request.address());
  }

  public CustomerResponse toResponse(Customer customer) {
    return new CustomerResponse(customer.getId(), customer.getFirstName(), customer.getLastName(), customer.getFullName(), customer.getEmail(), customer.getPhone(), customer.getAddress());
  }

  private NameParts names(CustomerRequest request) {
    String firstName = normalize(request.firstName());
    String lastName = normalize(request.lastName());
    if (firstName != null && lastName != null) {
      return new NameParts(firstName, lastName, firstName + " " + lastName);
    }

    String fullName = normalize(request.fullName());
    if (fullName == null) {
      throw new IllegalArgumentException("Customer first and last name are required");
    }
    int splitAt = fullName.indexOf(' ');
    if (splitAt < 0) {
      return new NameParts(fullName, fullName, fullName);
    }
    return new NameParts(fullName.substring(0, splitAt).trim(), fullName.substring(splitAt + 1).trim(), fullName);
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record NameParts(String firstName, String lastName, String fullName) {
  }
}
