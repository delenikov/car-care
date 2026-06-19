package com.delenicode.carcare.appointment.service;

import com.delenicode.carcare.appointment.dto.request.PublicAppointmentRequest;
import com.delenicode.carcare.appointment.exception.InvalidAppointmentException;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import com.delenicode.carcare.vehicle.model.Vehicle;
import com.delenicode.carcare.vehicle.repository.VehicleRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicAppointmentBookingService {
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;

  public Customer resolveCustomer(PublicAppointmentRequest request) {
    return customers.findByEmailIgnoreCaseAndDeletedFalse(request.email().trim())
        .orElseGet(() -> customers.save(newCustomer(request)));
  }

  public Vehicle resolveVehicle(PublicAppointmentRequest request, Customer customer) {
    String plateNumber = request.plateNumber().trim().toUpperCase();
    return vehicles.findByPlateNumberIgnoreCaseAndCustomerDeletedFalse(plateNumber)
        .map(vehicle -> vehicleForCustomer(vehicle, customer))
        .orElseGet(() -> vehicles.save(newVehicle(request, customer, plateNumber)));
  }

  private Vehicle vehicleForCustomer(Vehicle vehicle, Customer customer) {
    if (!vehicle.getCustomer().getId().equals(customer.getId())) {
      throw new InvalidAppointmentException("Vehicle plate already belongs to another customer");
    }
    return vehicle;
  }

  private Customer newCustomer(PublicAppointmentRequest request) {
    Customer customer = new Customer();
    String fullName = request.fullName().trim();
    customer.setFullName(fullName);
    customer.setFirstName(firstName(fullName));
    customer.setLastName(lastName(fullName));
    customer.setEmail(request.email().trim().toLowerCase());
    customer.setPhone(request.phone());
    return customer;
  }

  private Vehicle newVehicle(PublicAppointmentRequest request, Customer customer, String plateNumber) {
    Vehicle vehicle = new Vehicle();
    vehicle.setCustomer(customer);
    vehicle.setPlateNumber(plateNumber);
    vehicle.setVin(blankToNull(request.vin()));
    vehicle.setMake(request.make().trim());
    vehicle.setModel(request.model().trim());
    vehicle.setModelYear(request.modelYear());
    vehicle.setEngine(blankToNull(request.engine()));
    vehicle.setFuelType(blankToNull(request.fuelType()));
    return vehicle;
  }

  private String firstName(String fullName) {
    String[] parts = fullName.split("\\s+");
    return parts.length == 0 ? fullName : parts[0];
  }

  private String lastName(String fullName) {
    String[] parts = fullName.split("\\s+");
    return parts.length <= 1 ? firstName(fullName) : String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
