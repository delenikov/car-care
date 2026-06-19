package com.delenicode.carcare.servicerecord;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
  @Query("select sr.id from ServiceRecord sr join sr.customer c where c.deleted = false")
  Page<Long> findPageIds(Pageable pageable);

  @EntityGraph(attributePaths = { "customer", "vehicle" })
  @Query("select sr from ServiceRecord sr where sr.id in :ids")
  List<ServiceRecord> findAllWithDetailsByIdIn(@Param("ids") List<Long> ids);

  @EntityGraph(attributePaths = { "customer", "vehicle" })
  @Query("select sr from ServiceRecord sr join sr.customer c where sr.id = :id and c.deleted = false")
  Optional<ServiceRecord> findByIdWithDetails(@Param("id") Long id);

  @EntityGraph(attributePaths = { "customer", "vehicle" })
  @Query("""
      select sr
      from ServiceRecord sr
      join sr.customer c
      where c.id = :customerId
        and c.deleted = false
      order by sr.serviceDate desc, sr.id desc
      """)
  List<ServiceRecord> findByCustomerIdAndCustomerDeletedFalse(@Param("customerId") Long customerId);

  @EntityGraph(attributePaths = { "customer", "vehicle" })
  @Query("""
      select sr
      from ServiceRecord sr
      join sr.vehicle v
      join v.customer c
      where v.id = :vehicleId
        and c.deleted = false
      order by sr.serviceDate desc, sr.id desc
      """)
  List<ServiceRecord> findByVehicleIdAndVehicleCustomerDeletedFalseOrderByServiceDateDesc(@Param("vehicleId") Long vehicleId);

  long countByCustomerIdAndCustomerDeletedFalse(Long customerId);
}
