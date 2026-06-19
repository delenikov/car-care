package com.delenicode.carcare.offer.repository;


import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.vehicle.model.Vehicle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfferRepository extends JpaRepository<Offer, Long> {
  @Query("select o.id from Offer o")
  Page<Long> findPageIds(Pageable pageable);

  @EntityGraph(attributePaths = { "customer", "vehicle", "parts" })
  @Query("select distinct o from Offer o where o.id in :ids")
  List<Offer> findAllWithDetailsByIdIn(@Param("ids") List<Long> ids);

  @EntityGraph(attributePaths = { "customer", "vehicle", "parts" })
  @Query("select o from Offer o where o.id = :id")
  Optional<Offer> findByIdWithDetails(@Param("id") Long id);
}
