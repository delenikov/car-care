package com.delenicode.carcare.offer;

import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.vehicle.Vehicle;
import com.delenicode.carcare.vehicle.VehicleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfferService {
  private final OfferRepository offers;
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;

  @Transactional(readOnly = true)
  public List<OfferResponse> findAll() {
    return offers.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public OfferResponse create(OfferRequest request) {
    Offer offer = new Offer();
    offer.setCustomer(customers.findById(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found")));
    if (request.vehicleId() != null) {
      offer.setVehicle(vehicles.findById(request.vehicleId()).orElseThrow(() -> new IllegalArgumentException("Vehicle not found")));
    }
    offer.setTitle(request.title());
    offer.setDescription(request.description());
    offer.setAmount(request.amount());
    offer.setExpiresOn(request.expiresOn());
    return toResponse(offers.save(offer));
  }

  public OfferResponse toResponse(Offer offer) {
    Vehicle vehicle = offer.getVehicle();
    return new OfferResponse(offer.getId(), offer.getCustomer().getId(), vehicle == null ? null : vehicle.getId(), offer.getTitle(), offer.getDescription(), offer.getAmount(), offer.getExpiresOn(), offer.getStatus());
  }
}
