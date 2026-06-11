package com.delenicode.carcare.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
  boolean existsByPlateNumber(String plateNumber);
}
