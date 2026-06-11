package com.delenicode.carcare.appointment;

import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.vehicle.VehicleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {
  private final AppointmentRepository appointments;
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;

  @Transactional(readOnly = true)
  public List<AppointmentResponse> findAll() {
    return appointments.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public AppointmentResponse create(AppointmentRequest request) {
    Appointment appointment = new Appointment();
    appointment.setCustomer(customers.findById(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found")));
    appointment.setVehicle(vehicles.findById(request.vehicleId()).orElseThrow(() -> new IllegalArgumentException("Vehicle not found")));
    appointment.setScheduledAt(request.scheduledAt());
    appointment.setServiceType(request.serviceType());
    appointment.setNotes(request.notes());
    return toResponse(appointments.save(appointment));
  }

  public AppointmentResponse toResponse(Appointment appointment) {
    return new AppointmentResponse(appointment.getId(), appointment.getCustomer().getId(), appointment.getVehicle().getId(), appointment.getScheduledAt(), appointment.getServiceType(), appointment.getStatus(), appointment.getNotes());
  }
}
