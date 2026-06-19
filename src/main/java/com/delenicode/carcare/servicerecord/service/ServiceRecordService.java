package com.delenicode.carcare.servicerecord.service;


import com.delenicode.carcare.customer.exception.CustomerNotFoundException;
import com.delenicode.carcare.servicerecord.dto.request.ServiceRecordRequest;
import com.delenicode.carcare.servicerecord.dto.response.ServiceRecordResponse;
import com.delenicode.carcare.servicerecord.event.ServiceRecordCreatedEvent;
import com.delenicode.carcare.servicerecord.exception.InvalidServiceRecordException;
import com.delenicode.carcare.servicerecord.exception.ServiceRecordNotFoundException;
import com.delenicode.carcare.servicerecord.mapper.ServiceRecordMapper;
import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import com.delenicode.carcare.servicerecord.repository.ServiceRecordRepository;
import com.delenicode.carcare.vehicle.exception.VehicleNotFoundException;
import com.delenicode.carcare.vehicle.repository.VehicleRepository;
import com.delenicode.carcare.common.PageResponse;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import com.delenicode.carcare.vehicle.model.Vehicle;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRecordService {
  private final ServiceRecordRepository serviceRecords;
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;
  private final ApplicationEventPublisher events;
  private final ServiceRecordCostService costs;
  private final ServiceRecordMapper mapper;

  @Transactional(readOnly = true)
  public PageResponse<ServiceRecordResponse> findAll(Pageable pageable) {
    Page<Long> page = serviceRecords.findPageIds(pageable);
    if (page.isEmpty()) {
      return PageResponse.from(page, List.of());
    }
    Map<Long, ServiceRecord> recordsById = new LinkedHashMap<>();
    serviceRecords.findAllWithDetailsByIdIn(page.getContent())
        .forEach(record -> recordsById.put(record.getId(), record));
    List<ServiceRecordResponse> content = page.getContent().stream()
        .map(recordsById::get)
        .filter(Objects::nonNull)
        .map(mapper::toResponse)
        .toList();
    return PageResponse.from(page, content);
  }

  @Transactional(readOnly = true)
  public ServiceRecordResponse findById(Long id) {
    return serviceRecords.findByIdWithDetails(id)
        .map(mapper::toResponse)
        .orElseThrow(() -> new ServiceRecordNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public List<ServiceRecordResponse> findByCustomerId(Long customerId) {
    return serviceRecords.findByCustomerIdAndCustomerDeletedFalse(customerId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ServiceRecordResponse> findByVehicleId(Long vehicleId) {
    return serviceRecords.findByVehicleIdAndVehicleCustomerDeletedFalseOrderByServiceDateDesc(vehicleId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Transactional
  public ServiceRecordResponse create(ServiceRecordRequest request) {
    Customer customer = customers.findByIdAndDeletedFalse(request.customerId())
        .orElseThrow(() -> new CustomerNotFoundException(request.customerId()));
    Vehicle vehicle = resolveVehicle(request.vehicleId(), customer);
    BigDecimal partsCost = costs.partsCost(request);
    BigDecimal laborCost = costs.laborCost(request);
    ServiceRecord record = new ServiceRecord();
    record.setCustomer(customer);
    record.setVehicle(vehicle);
    record.setServiceDate(request.serviceDate());
    record.setServiceType(request.serviceType());
    record.setPartsCost(partsCost);
    record.setLaborCost(laborCost);
    record.setTotalAmount(costs.totalAmount(partsCost, laborCost));
    record.setOdometer(request.odometer());
    record.setReplacedParts(request.replacedParts());
    record.setNotes(request.notes());
    ServiceRecord saved = serviceRecords.save(record);
    log.info("Service record created. Service record ID: {}. Customer ID: {}. Vehicle ID: {}. Service date: {}. Total amount: {}", saved.getId(), customer.getId(), vehicle.getId(), saved.getServiceDate(), saved.getTotalAmount());
    log.info("Service record created event published. Service record ID: {}", saved.getId());
    events.publishEvent(new ServiceRecordCreatedEvent(saved.getId()));
    return mapper.toResponse(saved);
  }

  private Vehicle resolveVehicle(Long vehicleId, Customer customer) {
    Vehicle vehicle = vehicles.findById(vehicleId)
        .filter(existing -> !existing.getCustomer().isDeleted())
        .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
    if (!Objects.equals(vehicle.getCustomer().getId(), customer.getId())) {
      throw new InvalidServiceRecordException("Vehicle does not belong to customer");
    }
    return vehicle;
  }
}
