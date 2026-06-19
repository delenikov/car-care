package com.delenicode.carcare.dashboard.service;


import com.delenicode.carcare.appointment.model.Appointment;
import com.delenicode.carcare.appointment.repository.AppointmentRepository;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.dashboard.dto.response.DashboardSummary;
import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import com.delenicode.carcare.vehicle.model.Vehicle;
import com.delenicode.carcare.vehicle.repository.VehicleRepository;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import com.delenicode.carcare.offer.repository.OfferRepository;
import com.delenicode.carcare.servicerecord.repository.ServiceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;
  private final AppointmentRepository appointments;
  private final ServiceRecordRepository serviceRecords;
  private final OfferRepository offers;

  @Transactional(readOnly = true)
  public DashboardSummary summary() {
    return new DashboardSummary(customers.count(), vehicles.count(), appointments.count(), serviceRecords.count(), offers.count());
  }
}
