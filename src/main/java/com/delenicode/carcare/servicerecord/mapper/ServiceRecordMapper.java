package com.delenicode.carcare.servicerecord.mapper;


import com.delenicode.carcare.servicerecord.dto.response.ServiceRecordResponse;
import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import com.delenicode.carcare.vehicle.model.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class ServiceRecordMapper {
  public ServiceRecordResponse toResponse(ServiceRecord record) {
    Vehicle vehicle = record.getVehicle();
    return new ServiceRecordResponse(
        record.getId(),
        record.getCustomer().getId(),
        record.getCustomer().getFullName(),
        vehicle.getId(),
        vehicle.getPlateNumber(),
        vehicle.getMake() + " " + vehicle.getModel(),
        record.getServiceDate(),
        record.getServiceType(),
        record.getPartsCost(),
        record.getLaborCost(),
        record.getTotalAmount(),
        record.getOdometer(),
        record.getReplacedParts(),
        record.getNotes());
  }
}
