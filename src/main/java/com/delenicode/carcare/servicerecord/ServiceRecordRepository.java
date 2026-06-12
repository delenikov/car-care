package com.delenicode.carcare.servicerecord;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
  List<ServiceRecord> findByCustomerIdAndCustomerDeletedFalse(Long customerId);
  List<ServiceRecord> findByVehicleIdAndVehicleCustomerDeletedFalseOrderByServiceDateDesc(Long vehicleId);
}
