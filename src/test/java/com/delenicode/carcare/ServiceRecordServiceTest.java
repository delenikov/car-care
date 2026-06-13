package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.document.ServiceDocumentService;
import com.delenicode.carcare.servicerecord.ServiceRecord;
import com.delenicode.carcare.servicerecord.ServiceRecordRepository;
import com.delenicode.carcare.servicerecord.ServiceRecordRequest;
import com.delenicode.carcare.servicerecord.ServiceRecordService;
import com.delenicode.carcare.vehicle.Vehicle;
import com.delenicode.carcare.vehicle.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceRecordServiceTest {
  @Mock
  ServiceRecordRepository serviceRecords;

  @Mock
  CustomerRepository customers;

  @Mock
  VehicleRepository vehicles;

  @Mock
  ServiceDocumentService documents;

  ServiceRecordService serviceRecordService;

  @BeforeEach
  void setUp() {
    serviceRecordService = new ServiceRecordService(serviceRecords, customers, vehicles, documents);
  }

  @Test
  void createCalculatesTotalFromPartsAndLabor() {
    Customer customer = customer(10L);
    Vehicle vehicle = vehicle(20L, customer);
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(customer));
    when(vehicles.findById(20L)).thenReturn(Optional.of(vehicle));
    when(serviceRecords.save(any(ServiceRecord.class))).thenAnswer(invocation -> {
      ServiceRecord record = invocation.getArgument(0);
      record.setId(30L);
      return record;
    });

    var response = serviceRecordService.create(new ServiceRecordRequest(
        10L,
        20L,
        LocalDate.of(2026, 6, 12),
        "Minor Service",
        new BigDecimal("1200.00"),
        new BigDecimal("800.00"),
        null,
        123456,
        "Oil filter",
        "Oil and filters"));

    assertThat(response.partsCost()).isEqualByComparingTo("1200.00");
    assertThat(response.laborCost()).isEqualByComparingTo("800.00");
    assertThat(response.totalAmount()).isEqualByComparingTo("2000.00");
    assertThat(response.replacedParts()).isEqualTo("Oil filter");
    verify(documents).generateForServiceRecord(any(ServiceRecord.class));
  }

  @Test
  void createKeepsBackwardCompatibleTotalAsLaborCostWhenBreakdownMissing() {
    Customer customer = customer(10L);
    Vehicle vehicle = vehicle(20L, customer);
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(customer));
    when(vehicles.findById(20L)).thenReturn(Optional.of(vehicle));
    when(serviceRecords.save(any(ServiceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = serviceRecordService.create(new ServiceRecordRequest(
        10L,
        20L,
        LocalDate.of(2026, 6, 12),
        "Custom diagnostic",
        null,
        null,
        new BigDecimal("3500.00"),
        123456,
        null,
        "Legacy payload"));

    assertThat(response.partsCost()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.laborCost()).isEqualByComparingTo("3500.00");
    assertThat(response.totalAmount()).isEqualByComparingTo("3500.00");
  }

  @Test
  void createRejectsVehicleFromDifferentCustomer() {
    Customer requestCustomer = customer(10L);
    Customer owner = customer(11L);
    Vehicle vehicle = vehicle(20L, owner);
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(requestCustomer));
    when(vehicles.findById(20L)).thenReturn(Optional.of(vehicle));

    assertThatThrownBy(() -> serviceRecordService.create(new ServiceRecordRequest(
        10L,
        20L,
        LocalDate.of(2026, 6, 12),
        "Minor Service",
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null,
        123456,
        null,
        null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Vehicle does not belong to customer");
  }

  @Test
  void vehicleHistoryUsesVehicleHistoryQuery() {
    Customer customer = customer(10L);
    ServiceRecord record = new ServiceRecord();
    record.setId(30L);
    record.setCustomer(customer);
    record.setVehicle(vehicle(20L, customer));
    record.setServiceDate(LocalDate.of(2026, 6, 12));
    record.setServiceType("Major Service");
    record.setPartsCost(BigDecimal.ONE);
    record.setLaborCost(BigDecimal.TEN);
    record.setTotalAmount(new BigDecimal("11.00"));
    when(serviceRecords.findByVehicleIdAndVehicleCustomerDeletedFalseOrderByServiceDateDesc(20L)).thenReturn(List.of(record));

    assertThat(serviceRecordService.findByVehicleId(20L)).extracting("serviceType").containsExactly("Major Service");
  }

  @Test
  void findByIdReturnsFullServiceRecordDetails() {
    Customer customer = customer(10L);
    ServiceRecord record = serviceRecord(30L, customer, vehicle(20L, customer));
    when(serviceRecords.findById(30L)).thenReturn(Optional.of(record));

    var response = serviceRecordService.findById(30L);

    assertThat(response.customerId()).isEqualTo(10L);
    assertThat(response.vehicleId()).isEqualTo(20L);
    assertThat(response.serviceDate()).isEqualTo(LocalDate.of(2026, 6, 12));
    assertThat(response.serviceType()).isEqualTo("Major Service");
    assertThat(response.partsCost()).isEqualByComparingTo("1200.00");
    assertThat(response.laborCost()).isEqualByComparingTo("800.00");
    assertThat(response.totalAmount()).isEqualByComparingTo("2000.00");
    assertThat(response.odometer()).isEqualTo(123456);
    assertThat(response.replacedParts()).isEqualTo("Air filter");
    assertThat(response.notes()).isEqualTo("Full detail");
  }

  @Test
  void findByIdRejectsDeletedCustomerRecord() {
    Customer customer = customer(10L);
    customer.setDeleted(true);
    when(serviceRecords.findById(30L)).thenReturn(Optional.of(serviceRecord(30L, customer, vehicle(20L, customer))));

    assertThatThrownBy(() -> serviceRecordService.findById(30L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Service record not found");
  }

  private Customer customer(Long id) {
    Customer customer = new Customer();
    customer.setId(id);
    customer.setFirstName("Ada");
    customer.setLastName("Lovelace");
    customer.setFullName("Ada Lovelace");
    customer.setEmail("ada-" + id + "@carcare.test");
    return customer;
  }

  private Vehicle vehicle(Long id, Customer customer) {
    Vehicle vehicle = new Vehicle();
    vehicle.setId(id);
    vehicle.setCustomer(customer);
    vehicle.setPlateNumber("SK-" + id);
    vehicle.setMake("Volkswagen");
    vehicle.setModel("Golf");
    vehicle.setModelYear(2020);
    return vehicle;
  }

  private ServiceRecord serviceRecord(Long id, Customer customer, Vehicle vehicle) {
    ServiceRecord record = new ServiceRecord();
    record.setId(id);
    record.setCustomer(customer);
    record.setVehicle(vehicle);
    record.setServiceDate(LocalDate.of(2026, 6, 12));
    record.setServiceType("Major Service");
    record.setPartsCost(new BigDecimal("1200.00"));
    record.setLaborCost(new BigDecimal("800.00"));
    record.setTotalAmount(new BigDecimal("2000.00"));
    record.setOdometer(123456);
    record.setReplacedParts("Air filter");
    record.setNotes("Full detail");
    return record;
  }
}
