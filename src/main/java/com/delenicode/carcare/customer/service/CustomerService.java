package com.delenicode.carcare.customer.service;


import com.delenicode.carcare.customer.dto.request.CustomerRequest;
import com.delenicode.carcare.customer.dto.response.CustomerResponse;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import com.delenicode.carcare.common.LogSanitizer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {
  private final CustomerRepository customers;

  @Transactional(readOnly = true)
  public List<CustomerResponse> findAll() {
    return customers.findByDeletedFalse().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<CustomerResponse> search(String query) {
    if (hasText(query)) {
      return customers.findBySearchTerm(query.trim()).stream().map(this::toResponse).toList();
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
    Customer saved = customers.save(customer);
    log.info("Customer created. Customer ID: {}. Email: {}", saved.getId(), LogSanitizer.email(saved.getEmail()));
    return toResponse(saved);
  }

  @Transactional
  public CustomerResponse update(Long id, CustomerRequest request) {
    Customer customer = customers.findByIdAndDeletedFalse(id).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    apply(customer, request);
    Customer saved = customers.save(customer);
    log.info("Customer updated. Customer ID: {}. Email: {}", saved.getId(), LogSanitizer.email(saved.getEmail()));
    return toResponse(saved);
  }

  @Transactional
  public void delete(Long id) {
    Customer customer = customers.findByIdAndDeletedFalse(id).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    customer.setDeleted(true);
    customers.save(customer);
    log.info("Customer deleted. Customer ID: {}", customer.getId());
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

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private record NameParts(String firstName, String lastName, String fullName) {
  }
}
