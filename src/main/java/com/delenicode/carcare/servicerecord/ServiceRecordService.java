package com.delenicode.carcare.servicerecord;

import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.vehicle.VehicleRepository;
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

  @Transactional(readOnly = true)
  public List<ServiceRecordResponse> findAll() {
    return serviceRecords.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public ServiceRecordResponse create(ServiceRecordRequest request) {
    ServiceRecord record = new ServiceRecord();
    record.setCustomer(customers.findById(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found")));
    record.setVehicle(vehicles.findById(request.vehicleId()).orElseThrow(() -> new IllegalArgumentException("Vehicle not found")));
    record.setServiceDate(request.serviceDate());
    record.setServiceType(request.serviceType());
    record.setTotalAmount(request.totalAmount());
    record.setOdometer(request.odometer());
    record.setNotes(request.notes());
    return toResponse(serviceRecords.save(record));
  }

  public ServiceRecordResponse toResponse(ServiceRecord record) {
    return new ServiceRecordResponse(record.getId(), record.getCustomer().getId(), record.getVehicle().getId(), record.getServiceDate(), record.getServiceType(), record.getTotalAmount(), record.getOdometer(), record.getNotes());
  }
}
