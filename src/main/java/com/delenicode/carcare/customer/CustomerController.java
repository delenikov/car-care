package com.delenicode.carcare.customer;

import com.delenicode.carcare.common.ApiResponse;
import com.delenicode.carcare.loyalty.CustomerLoyaltyService;
import com.delenicode.carcare.loyalty.CustomerLoyaltyStatusResponse;
import com.delenicode.carcare.servicerecord.ServiceRecordResponse;
import com.delenicode.carcare.servicerecord.ServiceRecordService;
import com.delenicode.carcare.vehicle.VehicleResponse;
import com.delenicode.carcare.vehicle.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {
  private final CustomerService customers;
  private final VehicleService vehicles;
  private final ServiceRecordService serviceRecords;
  private final CustomerLoyaltyService loyalty;

  @GetMapping
  ApiResponse<List<CustomerResponse>> all(@RequestParam(required = false) String firstName, @RequestParam(required = false) String lastName) {
    return ApiResponse.ok("Customers loaded", customers.search(firstName, lastName));
  }

  @GetMapping("/{id}")
  ApiResponse<CustomerResponse> one(@PathVariable Long id) {
    return ApiResponse.ok("Customer loaded", customers.findById(id));
  }

  @GetMapping("/{id}/vehicles")
  ApiResponse<List<VehicleResponse>> vehicles(@PathVariable Long id) {
    customers.findById(id);
    return ApiResponse.ok("Customer vehicles loaded", vehicles.findByCustomerId(id));
  }

  @GetMapping("/{id}/service-history")
  ApiResponse<List<ServiceRecordResponse>> serviceHistory(@PathVariable Long id) {
    customers.findById(id);
    return ApiResponse.ok("Customer service history loaded", serviceRecords.findByCustomerId(id));
  }

  @GetMapping("/{id}/loyalty-status")
  ApiResponse<CustomerLoyaltyStatusResponse> loyaltyStatus(@PathVariable Long id) {
    customers.findById(id);
    return ApiResponse.ok("Customer loyalty status loaded", loyalty.statusForCustomer(id));
  }

  @PostMapping
  ApiResponse<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
    return ApiResponse.ok("Customer created", customers.create(request));
  }

  @PutMapping("/{id}")
  ApiResponse<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
    return ApiResponse.ok("Customer updated", customers.update(id, request));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Void> delete(@PathVariable Long id) {
    customers.delete(id);
    return ApiResponse.ok("Customer deleted", null);
  }
}
