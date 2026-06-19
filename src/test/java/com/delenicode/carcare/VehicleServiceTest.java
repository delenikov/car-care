package com.delenicode.carcare;


import com.delenicode.carcare.vehicle.dto.request.VehicleRequest;
import com.delenicode.carcare.vehicle.repository.VehicleRepository;
import com.delenicode.carcare.vehicle.service.VehicleService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import com.delenicode.carcare.vehicle.model.Vehicle;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {
  @Mock
  VehicleRepository vehicles;

  @Mock
  CustomerRepository customers;

  VehicleService vehicleService;

  @BeforeEach
  void setUp() {
    vehicleService = new VehicleService(vehicles, customers);
  }

  @Test
  void createStoresVehicleFieldsAndCustomerAssociation() {
    Customer customer = customer(10L, "Ada Lovelace");
    when(vehicles.existsByPlateNumber("SK-1234-AA")).thenReturn(false);
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(customer));
    when(vehicles.save(any(Vehicle.class))).thenAnswer(invocation -> {
      Vehicle vehicle = invocation.getArgument(0);
      vehicle.setId(20L);
      return vehicle;
    });

    var response = vehicleService.create(new VehicleRequest(10L, "SK-1234-AA", "Volkswagen", "Golf", 2020, "VIN123", "Diesel", "2.0 TDI"));

    assertThat(response.customerId()).isEqualTo(10L);
    assertThat(response.customerName()).isEqualTo("Ada Lovelace");
    assertThat(response.plateNumber()).isEqualTo("SK-1234-AA");
    assertThat(response.make()).isEqualTo("Volkswagen");
    assertThat(response.model()).isEqualTo("Golf");
    assertThat(response.modelYear()).isEqualTo(2020);
    assertThat(response.vin()).isEqualTo("VIN123");
    assertThat(response.fuelType()).isEqualTo("Diesel");
    assertThat(response.engine()).isEqualTo("2.0 TDI");
  }

  @Test
  void updateReassignsVehicleToAnotherCustomer() {
    Customer oldCustomer = customer(10L, "Ada Lovelace");
    Customer newCustomer = customer(11L, "Grace Hopper");
    Vehicle vehicle = vehicle(20L, oldCustomer, "SK-1234-AA", "VIN123");
    when(vehicles.findById(20L)).thenReturn(Optional.of(vehicle));
    when(vehicles.existsByPlateNumberAndIdNot("SK-7777-AA", 20L)).thenReturn(false);
    when(customers.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(newCustomer));
    when(vehicles.save(vehicle)).thenReturn(vehicle);

    var response = vehicleService.update(20L, new VehicleRequest(11L, "SK-7777-AA", "Toyota", "Corolla", 2022, "VIN777", "Hybrid", "1.8"));

    assertThat(response.customerId()).isEqualTo(11L);
    assertThat(response.plateNumber()).isEqualTo("SK-7777-AA");
    assertThat(response.vin()).isEqualTo("VIN777");
    assertThat(response.fuelType()).isEqualTo("Hybrid");
    assertThat(response.engine()).isEqualTo("1.8");
  }

  @Test
  void updateRejectsDuplicatePlate() {
    Customer customer = customer(10L, "Ada Lovelace");
    Vehicle vehicle = vehicle(20L, customer, "SK-1234-AA", "VIN123");
    when(vehicles.findById(20L)).thenReturn(Optional.of(vehicle));
    when(vehicles.existsByPlateNumberAndIdNot("SK-9999-AA", 20L)).thenReturn(true);

    assertThatThrownBy(() -> vehicleService.update(20L, new VehicleRequest(10L, "SK-9999-AA", "Toyota", "Corolla", 2022, "VIN999", "Petrol", "1.6")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Vehicle plate already exists");
  }

  @Test
  void searchRoutesToVinPlateOrOwnerQueries() {
    Customer customer = customer(10L, "Ada Lovelace");
    Vehicle vehicle = vehicle(20L, customer, "SK-1234-AA", "VIN123");
    when(vehicles.findBySearchTerm("Ada")).thenReturn(List.of(vehicle));
    when(vehicles.findByVinContainingIgnoreCaseAndCustomerDeletedFalse("VIN")).thenReturn(List.of(vehicle));
    when(vehicles.findByPlateNumberContainingIgnoreCaseAndCustomerDeletedFalse("SK")).thenReturn(List.of(vehicle));
    when(vehicles.findByOwnerName("Ada")).thenReturn(List.of(vehicle));

    assertThat(vehicleService.search("Ada", null, null, null)).extracting("customerName").containsExactly("Ada Lovelace");
    assertThat(vehicleService.search(null, "VIN", null, null)).extracting("vin").containsExactly("VIN123");
    assertThat(vehicleService.search(null, null, "SK", null)).extracting("plateNumber").containsExactly("SK-1234-AA");
    assertThat(vehicleService.search(null, null, null, "Ada")).extracting("customerId").containsExactly(10L);
  }

  private Customer customer(Long id, String fullName) {
    String[] parts = fullName.split(" ", 2);
    Customer customer = new Customer();
    customer.setId(id);
    customer.setFirstName(parts[0]);
    customer.setLastName(parts.length > 1 ? parts[1] : parts[0]);
    customer.setFullName(fullName);
    customer.setEmail(parts[0].toLowerCase() + "@carcare.test");
    return customer;
  }

  private Vehicle vehicle(Long id, Customer customer, String plateNumber, String vin) {
    Vehicle vehicle = new Vehicle();
    vehicle.setId(id);
    vehicle.setCustomer(customer);
    vehicle.setPlateNumber(plateNumber);
    vehicle.setMake("Volkswagen");
    vehicle.setModel("Golf");
    vehicle.setModelYear(2020);
    vehicle.setVin(vin);
    vehicle.setFuelType("Diesel");
    vehicle.setEngine("2.0 TDI");
    return vehicle;
  }
}
