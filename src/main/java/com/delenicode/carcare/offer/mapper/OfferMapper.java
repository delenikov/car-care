package com.delenicode.carcare.offer.mapper;


import com.delenicode.carcare.offer.dto.response.OfferPartResponse;
import com.delenicode.carcare.offer.dto.response.OfferResponse;
import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.offer.service.OfferPricingService;
import com.delenicode.carcare.vehicle.model.Vehicle;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfferMapper {
  private final OfferPricingService pricing;

  public OfferResponse toResponse(Offer offer) {
    Vehicle vehicle = offer.getVehicle();
    List<OfferPartResponse> parts = offer.getParts().stream()
        .map(part -> new OfferPartResponse(part.getName(), part.getPrice()))
        .toList();
    return new OfferResponse(
        offer.getId(),
        offer.getCustomer().getId(),
        offer.getCustomer().getFullName(),
        vehicle == null ? null : vehicle.getId(),
        vehicle == null ? null : vehicle.getPlateNumber(),
        vehicle == null ? null : vehicle.getMake() + " " + vehicle.getModel(),
        offer.getTitle(),
        offer.getDescription(),
        parts,
        offer.getPartsCost(),
        offer.getLaborCost(),
        pricing.subtotalAmount(offer),
        pricing.discountPercent(offer),
        pricing.discountAmount(offer),
        pricing.amount(offer),
        offer.getExpiresOn(),
        offer.getStatus());
  }
}
