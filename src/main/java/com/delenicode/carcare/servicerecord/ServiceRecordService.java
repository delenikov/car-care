package com.delenicode.carcare.servicerecord;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.document.ServiceDocumentService;
import com.delenicode.carcare.vehicle.Vehicle;
import com.delenicode.carcare.vehicle.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceRecordService {
  private final ServiceRecordRepository serviceRecords;
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;
  private final ServiceDocumentService documents;

  @Transactional(readOnly = true)
  public List<ServiceRecordResponse> findAll() {
    return serviceRecords.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<ServiceRecordResponse> findByCustomerId(Long customerId) {
    return serviceRecords.findByCustomerIdAndCustomerDeletedFalse(customerId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<ServiceRecordResponse> findByVehicleId(Long vehicleId) {
    return serviceRecords.findByVehicleIdAndVehicleCustomerDeletedFalseOrderByServiceDateDesc(vehicleId).stream().map(this::toResponse).toList();
  }

  @Transactional
  public ServiceRecordResponse create(ServiceRecordRequest request) {
    Customer customer = customers.findByIdAndDeletedFalse(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    Vehicle vehicle = vehicles.findById(request.vehicleId()).filter(existing -> !existing.getCustomer().isDeleted()).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
    if (!vehicle.getCustomer().getId().equals(customer.getId())) {
      throw new IllegalArgumentException("Vehicle does not belong to customer");
    }
    BigDecimal partsCost = costOrZero(request.partsCost());
    BigDecimal laborCost = costOrFallback(request.laborCost(), request.totalAmount());
    rejectNegative(partsCost, "Parts cost must not be negative");
    rejectNegative(laborCost, "Labor cost must not be negative");
    ServiceRecord record = new ServiceRecord();
    record.setCustomer(customer);
    record.setVehicle(vehicle);
    record.setServiceDate(request.serviceDate());
    record.setServiceType(request.serviceType());
    record.setPartsCost(partsCost);
    record.setLaborCost(laborCost);
    record.setTotalAmount(partsCost.add(laborCost));
    record.setOdometer(request.odometer());
    record.setReplacedParts(request.replacedParts());
    record.setNotes(request.notes());
    ServiceRecord saved = serviceRecords.save(record);
    documents.generateForServiceRecord(saved);
    return toResponse(saved);
  }

  public ServiceRecordResponse toResponse(ServiceRecord record) {
    return new ServiceRecordResponse(record.getId(), record.getCustomer().getId(), record.getVehicle().getId(), record.getServiceDate(), record.getServiceType(), record.getPartsCost(), record.getLaborCost(), record.getTotalAmount(), record.getOdometer(), record.getReplacedParts(), record.getNotes());
  }

  private BigDecimal costOrZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal costOrFallback(BigDecimal value, BigDecimal fallback) {
    if (value != null) {
      return value;
    }
    return fallback == null ? BigDecimal.ZERO : fallback;
  }

  private void rejectNegative(BigDecimal value, String message) {
    if (value.signum() < 0) {
      throw new IllegalArgumentException(message);
    }
  }
}
