package com.delenicode.carcare.vehicle;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
  boolean existsByPlateNumber(String plateNumber);
  boolean existsByPlateNumberAndIdNot(String plateNumber, Long id);
  List<Vehicle> findByCustomerIdAndCustomerDeletedFalse(Long customerId);
  List<Vehicle> findByCustomerDeletedFalse();
  List<Vehicle> findByVinContainingIgnoreCaseAndCustomerDeletedFalse(String vin);
  List<Vehicle> findByPlateNumberContainingIgnoreCaseAndCustomerDeletedFalse(String plateNumber);

  @Query("""
      select vehicle from Vehicle vehicle
      where vehicle.customer.deleted = false
        and (
          lower(vehicle.plateNumber) like lower(concat('%', :query, '%'))
          or lower(vehicle.vin) like lower(concat('%', :query, '%'))
          or lower(vehicle.customer.firstName) like lower(concat('%', :query, '%'))
          or lower(vehicle.customer.lastName) like lower(concat('%', :query, '%'))
          or lower(vehicle.customer.fullName) like lower(concat('%', :query, '%'))
        )
      """)
  List<Vehicle> findBySearchTerm(@Param("query") String query);

  @Query("""
      select vehicle from Vehicle vehicle
      where vehicle.customer.deleted = false
        and (
          lower(vehicle.customer.firstName) like lower(concat('%', :owner, '%'))
          or lower(vehicle.customer.lastName) like lower(concat('%', :owner, '%'))
          or lower(vehicle.customer.fullName) like lower(concat('%', :owner, '%'))
        )
      """)
  List<Vehicle> findByOwnerName(@Param("owner") String owner);
}
