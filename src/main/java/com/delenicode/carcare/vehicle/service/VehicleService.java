package com.delenicode.carcare.vehicle.service;


import com.delenicode.carcare.vehicle.dto.request.VehicleRequest;
import com.delenicode.carcare.vehicle.dto.response.VehicleResponse;
import com.delenicode.carcare.vehicle.model.Vehicle;
import com.delenicode.carcare.vehicle.repository.VehicleRepository;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {
  private final VehicleRepository vehicles;
  private final CustomerRepository customers;

  @Transactional(readOnly = true)
  public List<VehicleResponse> findAll() {
    return vehicles.findByCustomerDeletedFalse().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<VehicleResponse> search(String query, String vin, String plateNumber, String owner) {
    if (hasText(query)) {
      return vehicles.findBySearchTerm(query.trim()).stream().map(this::toResponse).toList();
    }
    if (hasText(vin)) {
      return vehicles.findByVinContainingIgnoreCaseAndCustomerDeletedFalse(vin.trim()).stream().map(this::toResponse).toList();
    }
    if (hasText(plateNumber)) {
      return vehicles.findByPlateNumberContainingIgnoreCaseAndCustomerDeletedFalse(plateNumber.trim()).stream().map(this::toResponse).toList();
    }
    if (hasText(owner)) {
      return vehicles.findByOwnerName(owner.trim()).stream().map(this::toResponse).toList();
    }
    return findAll();
  }

  @Transactional(readOnly = true)
  public VehicleResponse findById(Long id) {
    return vehicles.findById(id).filter(vehicle -> !vehicle.getCustomer().isDeleted()).map(this::toResponse).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
  }

  @Transactional(readOnly = true)
  public List<VehicleResponse> findByCustomerId(Long customerId) {
    return vehicles.findByCustomerIdAndCustomerDeletedFalse(customerId).stream().map(this::toResponse).toList();
  }

  @Transactional
  public VehicleResponse create(VehicleRequest request) {
    if (vehicles.existsByPlateNumber(request.plateNumber())) {
      throw new IllegalArgumentException("Vehicle plate already exists");
    }
    Customer customer = customers.findByIdAndDeletedFalse(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    Vehicle vehicle = new Vehicle();
    apply(vehicle, customer, request);
    Vehicle saved = vehicles.save(vehicle);
    log.info("Vehicle created. Vehicle ID: {}. Customer ID: {}. Plate: {}", saved.getId(), customer.getId(), saved.getPlateNumber());
    return toResponse(saved);
  }

  @Transactional
  public VehicleResponse update(Long id, VehicleRequest request) {
    Vehicle vehicle = vehicles.findById(id).filter(existing -> !existing.getCustomer().isDeleted()).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
    if (vehicles.existsByPlateNumberAndIdNot(request.plateNumber(), id)) {
      throw new IllegalArgumentException("Vehicle plate already exists");
    }
    Customer customer = customers.findByIdAndDeletedFalse(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    apply(vehicle, customer, request);
    Vehicle saved = vehicles.save(vehicle);
    log.info("Vehicle updated. Vehicle ID: {}. Customer ID: {}. Plate: {}", saved.getId(), customer.getId(), saved.getPlateNumber());
    return toResponse(saved);
  }

  public VehicleResponse toResponse(Vehicle vehicle) {
    return new VehicleResponse(
        vehicle.getId(),
        vehicle.getCustomer().getId(),
        vehicle.getCustomer().getFullName(),
        vehicle.getPlateNumber(),
        vehicle.getMake(),
        vehicle.getModel(),
        vehicle.getModelYear(),
        vehicle.getVin(),
        vehicle.getFuelType(),
        vehicle.getEngine());
  }

  private void apply(Vehicle vehicle, Customer customer, VehicleRequest request) {
    vehicle.setCustomer(customer);
    vehicle.setPlateNumber(request.plateNumber());
    vehicle.setMake(request.make());
    vehicle.setModel(request.model());
    vehicle.setModelYear(request.modelYear());
    vehicle.setVin(request.vin());
    vehicle.setFuelType(request.fuelType());
    vehicle.setEngine(request.engine());
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
