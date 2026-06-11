package com.delenicode.carcare.vehicle;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.customer.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleService {
  private final VehicleRepository vehicles;
  private final CustomerRepository customers;

  @Transactional(readOnly = true)
  public List<VehicleResponse> findAll() {
    return vehicles.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public VehicleResponse create(VehicleRequest request) {
    if (vehicles.existsByPlateNumber(request.plateNumber())) {
      throw new IllegalArgumentException("Vehicle plate already exists");
    }
    Customer customer = customers.findById(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    Vehicle vehicle = new Vehicle();
    vehicle.setCustomer(customer);
    vehicle.setPlateNumber(request.plateNumber());
    vehicle.setMake(request.make());
    vehicle.setModel(request.model());
    vehicle.setModelYear(request.modelYear());
    vehicle.setVin(request.vin());
    return toResponse(vehicles.save(vehicle));
  }

  public VehicleResponse toResponse(Vehicle vehicle) {
    return new VehicleResponse(vehicle.getId(), vehicle.getCustomer().getId(), vehicle.getPlateNumber(), vehicle.getMake(), vehicle.getModel(), vehicle.getModelYear(), vehicle.getVin());
  }
}
