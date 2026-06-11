package com.delenicode.carcare.customer;

import com.delenicode.carcare.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {
  private final CustomerService customers;

  @GetMapping
  ApiResponse<List<CustomerResponse>> all() {
    return ApiResponse.ok("Customers loaded", customers.findAll());
  }

  @GetMapping("/{id}")
  ApiResponse<CustomerResponse> one(@PathVariable Long id) {
    return ApiResponse.ok("Customer loaded", customers.findById(id));
  }

  @PostMapping
  ApiResponse<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
    return ApiResponse.ok("Customer created", customers.create(request));
  }

  @PutMapping("/{id}")
  ApiResponse<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
    return ApiResponse.ok("Customer updated", customers.update(id, request));
  }
}
