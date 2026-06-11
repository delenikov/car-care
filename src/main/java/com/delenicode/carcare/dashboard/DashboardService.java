package com.delenicode.carcare.dashboard;

import com.delenicode.carcare.appointment.AppointmentRepository;
import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.offer.OfferRepository;
import com.delenicode.carcare.servicerecord.ServiceRecordRepository;
import com.delenicode.carcare.vehicle.VehicleRepository;
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
